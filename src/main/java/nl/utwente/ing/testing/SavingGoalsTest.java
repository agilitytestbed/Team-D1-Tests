package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.CategoryRule;
import nl.utwente.ing.testing.bean.SavingGoal;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.IntervalHelper;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class SavingGoalsTest {

    @Test
    public void testGetSavingGoals() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/savingGoals").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/savingGoals").then().statusCode(401);

        // Test responses and status codes
        String newSessionID = RequestHelper.getNewSessionID();
        ArrayList<SavingGoal> savingGoals = new ArrayList<>();
        savingGoals.add(new SavingGoal("Holiday", 1000, 85, 100));
        savingGoals.add(new SavingGoal("Car", 2500, 250, 500));
        savingGoals.add(new SavingGoal("PC", 800, 100, 100));
        savingGoals.add(new SavingGoal("House", 300000, 5, 10));
        for (SavingGoal savingGoal : savingGoals) {
            RequestHelper.postSavingGoal(newSessionID, savingGoal);
        }

        String responseString = given().header("X-session-ID", newSessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(4));
        for (int i = 0; i <  savingGoals.size(); i++) {
            assertThat(responseList.get(i), hasKey("id"));
            assertThat(responseList.get(i).get("name"), equalTo(savingGoals.get(i).getName()));
            assertThat(responseList.get(i).get("goal"), equalTo(savingGoals.get(i).getGoal()));
            assertThat(responseList.get(i).get("savePerMonth"), equalTo(savingGoals.get(i).getSavePerMonth()));
            assertThat(responseList.get(i).get("minBalanceRequired"), equalTo(savingGoals.get(i).getMinBalanceRequired()));
            assertThat(responseList.get(i).get("balance"), equalTo(savingGoals.get(i).getBalance()));
        }
    }

    @Test
    public void testPostSavingGoalBasic() {
        String sessionID = RequestHelper.getNewSessionID();
        SavingGoal savingGoal = new SavingGoal("Holiday", 1000, 95, 100);

        // Test invalid session ID status code
        given().contentType("application/json").
                body(savingGoal).
                post(Constants.PREFIX + "/savingGoals").then().statusCode(401);
        given().contentType("application/json").
                body(savingGoal).header("X-session-ID", "A1B2C3D4E5").
                post(Constants.PREFIX + "/savingGoals").then().statusCode(401);

        // Test valid savingGoal post response and status code
        given().contentType("application/json").
                body(savingGoal).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/savingGoals").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("name", equalTo("Holiday")).
                body("goal", equalTo((float) 1000)).
                body("savePerMonth", equalTo((float) 95)).
                body("minBalanceRequired", equalTo((float) 100)).
                body("balance", equalTo((float) 0));

        // Test invalid input status code
        savingGoal.setName(null);
        given().contentType("application/json").
                body(savingGoal).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/savingGoals").then().statusCode(405);
    }

    @Test
    public void deleteSavingGoalBasic() {
        // Test invalid session ID status code
        when().delete(Constants.PREFIX + "/savingGoals/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").delete(Constants.PREFIX + "/savingGoals/1").then().statusCode(401);

        // Test invalid savingGoalID status code
        String sessionID = RequestHelper.getNewSessionID();
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/savingGoals/8381237").then().statusCode(404);

        // Test valid savingGoal status code
        SavingGoal savingGoal = new SavingGoal("Holiday", 1000, 100, 100);
        long savingGoalID = RequestHelper.postSavingGoal(sessionID, savingGoal);
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/savingGoals/" + savingGoalID).
                then().statusCode(204);

        // Test if the savingGoal is really deleted
        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(0));
    }

    @Test
    public void testSavingGoalsCorrectBalance() {
        String sessionID = RequestHelper.getNewSessionID();

        // JAN 2018
        // Starting balance: 0
        // Saving goals: PC = 0
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-01-18T07:30:18.028Z", 99, "NL00INGB0123456789", "deposit"));
        long pcSavingGoalID = RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "PC", 590, 100, 100));

        // Check for correct Saving Goals
        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(1));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 0));

        // FEB 2018
        // Starting balance: 99
        // Saving goals: PC = 0, Car = 0
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-02-18T07:30:18.028Z", 100, "NL00INGB0123456789", "deposit"));
        long carSavingGoalID = RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "Car", 2500, 250, 500));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(2));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 0));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));

        // MAR 2018
        // Starting balance: 199 - 100 = 99
        // Saving goals: PC = 100, Car = 0
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-03-18T07:30:18.028Z", 500, "NL00INGB0123456789", "deposit"));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(2));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 100));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));

        // APR 2018
        // Starting balance: 599 - 100 = 499
        // Saving goals: PC = 200, Car = 0, Holiday = 0
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-04-18T07:30:18.028Z", 100, "NL00INGB0123456789", "deposit"));
        long holidaySavingGoalID = RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "Holiday", 1000, 85, 90));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 200));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 0));

        // MAY 2018
        // Starting balance: 599 - 100 - 85 = 414
        // Saving goals: PC = 300, Car = 0, Holiday = 85
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-05-18T07:30:18.028Z", 4, "NL00INGB0123456789", "deposit"));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 300));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 85));

        // JUN 2018
        // Starting balance: 418 - 100 - 85 = 233
        // Saving goals: PC = 400, Car = 0, Holiday = 170
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-06-18T07:30:18.028Z", 1, "NL00INGB0123456789", "deposit"));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 400));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 170));

        // JUL 2018
        // Starting balance: 234 - 100 - 85 = 49
        // Saving goals: PC = 500, Car = 0, Holiday = 255
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-07-18T07:30:18.028Z", 130, "NL00INGB0123456789", "deposit"));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 500));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 255));

        // AUG 2018
        // Starting balance: 179 - 90 = 89
        // Saving goals: PC = 590, Car = 0, Holiday = 255
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-08-18T07:30:18.028Z", 1000, "NL00INGB0123456789", "deposit"));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 590));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 0));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 255));

        // SEP 2018
        // Starting balance: 1089 - 250 - 85 = 754
        // Saving goals: PC = 590, Car = 250, Holiday = 340
        RequestHelper.postTransaction(sessionID, new Transaction(
                "2018-09-18T07:30:18.028Z", 23, "NL00INGB0123456789", "deposit"));

        // Check for correct Saving Goals
        responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/savingGoals").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 590));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 250));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 340));
    }

    @Test
    public void testCorrectBalanceHistoryInteractionComplete() {
        String sessionID = RequestHelper.getNewSessionID();

        useReusableData(sessionID);

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "month").queryParam("intervals", 10).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(10));

        assertThat((Float) responseList.get(0).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("close"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("high"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("volume"), equalTo((float) 0));

        assertThat((Float) responseList.get(1).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(1).get("close"), equalTo((float) 99));
        assertThat((Float) responseList.get(1).get("high"), equalTo((float) 99));
        assertThat((Float) responseList.get(1).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(1).get("volume"), equalTo((float) 99));

        assertThat((Float) responseList.get(2).get("open"), equalTo((float) 99));
        assertThat((Float) responseList.get(2).get("close"), equalTo((float) 199));
        assertThat((Float) responseList.get(2).get("high"), equalTo((float) 199));
        assertThat((Float) responseList.get(2).get("low"), equalTo((float) 99));
        assertThat((Float) responseList.get(2).get("volume"), equalTo((float) 100));

        assertThat((Float) responseList.get(3).get("open"), equalTo((float) 199));
        assertThat((Float) responseList.get(3).get("close"), equalTo((float) 599));
        assertThat((Float) responseList.get(3).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(3).get("low"), equalTo((float) 99));
        assertThat((Float) responseList.get(3).get("volume"), equalTo((float) 600));

        assertThat((Float) responseList.get(4).get("open"), equalTo((float) 599));
        assertThat((Float) responseList.get(4).get("close"), equalTo((float) 599));
        assertThat((Float) responseList.get(4).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(4).get("low"), equalTo((float) 499));
        assertThat((Float) responseList.get(4).get("volume"), equalTo((float) 200));

        assertThat((Float) responseList.get(5).get("open"), equalTo((float) 599));
        assertThat((Float) responseList.get(5).get("close"), equalTo((float) 418));
        assertThat((Float) responseList.get(5).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(5).get("low"), equalTo((float) 414));
        assertThat((Float) responseList.get(5).get("volume"), equalTo((float) 189));

        assertThat((Float) responseList.get(6).get("open"), equalTo((float) 418));
        assertThat((Float) responseList.get(6).get("close"), equalTo((float) 234));
        assertThat((Float) responseList.get(6).get("high"), equalTo((float) 418));
        assertThat((Float) responseList.get(6).get("low"), equalTo((float) 233));
        assertThat((Float) responseList.get(6).get("volume"), equalTo((float) 186));

        assertThat((Float) responseList.get(7).get("open"), equalTo((float) 234));
        assertThat((Float) responseList.get(7).get("close"), equalTo((float) 179));
        assertThat((Float) responseList.get(7).get("high"), equalTo((float) 234));
        assertThat((Float) responseList.get(7).get("low"), equalTo((float) 49));
        assertThat((Float) responseList.get(7).get("volume"), equalTo((float) 315));

        assertThat((Float) responseList.get(8).get("open"), equalTo((float) 179));
        assertThat((Float) responseList.get(8).get("close"), equalTo((float) 1089));
        assertThat((Float) responseList.get(8).get("high"), equalTo((float) 1089));
        assertThat((Float) responseList.get(8).get("low"), equalTo((float) 89));
        assertThat((Float) responseList.get(8).get("volume"), equalTo((float) 1090));

        assertThat((Float) responseList.get(9).get("open"), equalTo((float) 1089));
        assertThat((Float) responseList.get(9).get("close"), equalTo((float) 777));
        assertThat((Float) responseList.get(9).get("high"), equalTo((float) 1089));
        assertThat((Float) responseList.get(9).get("low"), equalTo((float) 754));
        assertThat((Float) responseList.get(9).get("volume"), equalTo((float) 358));
    }

    @Test
    public void testCorrectBalanceHistoryInteractionPartial() {
        String sessionID = RequestHelper.getNewSessionID();

        useReusableData(sessionID);

        // Then test if it works for a part of the Transactions
        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "month").queryParam("intervals", 5).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(5));

        assertThat((Float) responseList.get(0).get("open"), equalTo((float) 599));
        assertThat((Float) responseList.get(0).get("close"), equalTo((float) 418));
        assertThat((Float) responseList.get(0).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(0).get("low"), equalTo((float) 414));
        assertThat((Float) responseList.get(0).get("volume"), equalTo((float) 189));

        assertThat((Float) responseList.get(1).get("open"), equalTo((float) 418));
        assertThat((Float) responseList.get(1).get("close"), equalTo((float) 234));
        assertThat((Float) responseList.get(1).get("high"), equalTo((float) 418));
        assertThat((Float) responseList.get(1).get("low"), equalTo((float) 233));
        assertThat((Float) responseList.get(1).get("volume"), equalTo((float) 186));

        assertThat((Float) responseList.get(2).get("open"), equalTo((float) 234));
        assertThat((Float) responseList.get(2).get("close"), equalTo((float) 179));
        assertThat((Float) responseList.get(2).get("high"), equalTo((float) 234));
        assertThat((Float) responseList.get(2).get("low"), equalTo((float) 49));
        assertThat((Float) responseList.get(2).get("volume"), equalTo((float) 315));

        assertThat((Float) responseList.get(3).get("open"), equalTo((float) 179));
        assertThat((Float) responseList.get(3).get("close"), equalTo((float) 1089));
        assertThat((Float) responseList.get(3).get("high"), equalTo((float) 1089));
        assertThat((Float) responseList.get(3).get("low"), equalTo((float) 89));
        assertThat((Float) responseList.get(3).get("volume"), equalTo((float) 1090));

        assertThat((Float) responseList.get(4).get("open"), equalTo((float) 1089));
        assertThat((Float) responseList.get(4).get("close"), equalTo((float) 777));
        assertThat((Float) responseList.get(4).get("high"), equalTo((float) 1089));
        assertThat((Float) responseList.get(4).get("low"), equalTo((float) 754));
        assertThat((Float) responseList.get(4).get("volume"), equalTo((float) 358));
    }

    @Test
    public void testCorrectBalanceHistoryAfterGoalDeletion() {
        String sessionID = RequestHelper.getNewSessionID();

        LocalDateTime localDateTimeNow = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime localDateTime = localDateTimeNow.minusMonths(9).minusHours(1);

        // JAN 2018
        // Starting balance: 0
        // Saving goals: PC = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 99, "NL00INGB0123456789", "deposit"));
        long pcSavingGoalID = RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "PC", 590, 100, 100));

        // FEB 2018
        // Starting balance: 99
        // Saving goals: PC = 0, Car = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 100, "NL00INGB0123456789", "deposit"));
        RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "Car", 2500, 250, 500));

        // MAR 2018
        // Starting balance: 199 - 100 = 99
        // Saving goals: PC = 100, Car = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 500, "NL00INGB0123456789", "deposit"));

        // APR 2018
        // Starting balance: 599 - 100 = 499
        // Saving goals: PC = 200, Car = 0, Holiday = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 100, "NL00INGB0123456789", "deposit"));
        RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "Holiday", 1000, 85, 90));

        // MAY 2018
        // Starting balance: 599 - 100 - 85 = 414
        // Saving goals: PC = 300, Car = 0, Holiday = 85
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 4, "NL00INGB0123456789", "deposit"));

        // JUN 2018
        // Starting balance: 418 - 100 - 85 = 233
        // Saving goals: PC = 400, Car = 0, Holiday = 170
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 1, "NL00INGB0123456789", "deposit"));

        // JUL 2018
        // Starting balance: 234 - 100 - 85 = 49
        // Saving goals: PC = 500, Car = 0, Holiday = 255
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 130, "NL00INGB0123456789", "deposit"));

        // AUG 2018
        // Starting balance: 179 - 90 = 89
        // Saving goals: PC = 590, Car = 0, Holiday = 255
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 1000, "NL00INGB0123456789", "deposit"));

        // Delete the first saving goal
        // Before deleting, balance: 89 + 1000 = 1089
        // After deleting, balance: 1089 + 590 = 1679
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/savingGoals/" + pcSavingGoalID).
                then().statusCode(204);

        // SEP 2018
        // Starting balance: 1679 - 250 - 85 = 1344
        // Saving goals: Car = 250, Holiday = 340
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 23, "NL00INGB0123456789", "deposit"));

        RequestHelper.postTransaction(sessionID, new Transaction(IntervalHelper.dateToString(localDateTimeNow), 1,
                "NL42INGB0123456789", "deposit"));

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "month").queryParam("intervals", 10).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(10));

        assertThat((Float) responseList.get(0).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("close"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("high"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("volume"), equalTo((float) 0));

        assertThat((Float) responseList.get(1).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(1).get("close"), equalTo((float) 99));
        assertThat((Float) responseList.get(1).get("high"), equalTo((float) 99));
        assertThat((Float) responseList.get(1).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(1).get("volume"), equalTo((float) 99));

        assertThat((Float) responseList.get(2).get("open"), equalTo((float) 99));
        assertThat((Float) responseList.get(2).get("close"), equalTo((float) 199));
        assertThat((Float) responseList.get(2).get("high"), equalTo((float) 199));
        assertThat((Float) responseList.get(2).get("low"), equalTo((float) 99));
        assertThat((Float) responseList.get(2).get("volume"), equalTo((float) 100));

        assertThat((Float) responseList.get(3).get("open"), equalTo((float) 199));
        assertThat((Float) responseList.get(3).get("close"), equalTo((float) 599));
        assertThat((Float) responseList.get(3).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(3).get("low"), equalTo((float) 99));
        assertThat((Float) responseList.get(3).get("volume"), equalTo((float) 600));

        assertThat((Float) responseList.get(4).get("open"), equalTo((float) 599));
        assertThat((Float) responseList.get(4).get("close"), equalTo((float) 599));
        assertThat((Float) responseList.get(4).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(4).get("low"), equalTo((float) 499));
        assertThat((Float) responseList.get(4).get("volume"), equalTo((float) 200));

        assertThat((Float) responseList.get(5).get("open"), equalTo((float) 599));
        assertThat((Float) responseList.get(5).get("close"), equalTo((float) 418));
        assertThat((Float) responseList.get(5).get("high"), equalTo((float) 599));
        assertThat((Float) responseList.get(5).get("low"), equalTo((float) 414));
        assertThat((Float) responseList.get(5).get("volume"), equalTo((float) 189));

        assertThat((Float) responseList.get(6).get("open"), equalTo((float) 418));
        assertThat((Float) responseList.get(6).get("close"), equalTo((float) 234));
        assertThat((Float) responseList.get(6).get("high"), equalTo((float) 418));
        assertThat((Float) responseList.get(6).get("low"), equalTo((float) 233));
        assertThat((Float) responseList.get(6).get("volume"), equalTo((float) 186));

        assertThat((Float) responseList.get(7).get("open"), equalTo((float) 234));
        assertThat((Float) responseList.get(7).get("close"), equalTo((float) 179));
        assertThat((Float) responseList.get(7).get("high"), equalTo((float) 234));
        assertThat((Float) responseList.get(7).get("low"), equalTo((float) 49));
        assertThat((Float) responseList.get(7).get("volume"), equalTo((float) 315));

        assertThat((Float) responseList.get(8).get("open"), equalTo((float) 179));
        assertThat((Float) responseList.get(8).get("close"), equalTo((float) 1679));
        assertThat((Float) responseList.get(8).get("high"), equalTo((float) 1679));
        assertThat((Float) responseList.get(8).get("low"), equalTo((float) 89));
        assertThat((Float) responseList.get(8).get("volume"), equalTo((float) 1680));

        assertThat((Float) responseList.get(9).get("open"), equalTo((float) 1679));
        assertThat((Float) responseList.get(9).get("close"), equalTo((float) 1368));
        assertThat((Float) responseList.get(9).get("high"), equalTo((float) 1679));
        assertThat((Float) responseList.get(9).get("low"), equalTo((float) 1344));
        assertThat((Float) responseList.get(9).get("volume"), equalTo((float) 359));
    }

    private void useReusableData(String sessionID) {
        LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).minusMonths(9).minusHours(1);

        // JAN 2018
        // Starting balance: 0
        // Saving goals: PC = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 99, "NL00INGB0123456789", "deposit"));
        RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "PC", 590, 100, 100));

        // FEB 2018
        // Starting balance: 99
        // Saving goals: PC = 0, Car = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 100, "NL00INGB0123456789", "deposit"));
        RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "Car", 2500, 250, 500));

        // MAR 2018
        // Starting balance: 199 - 100 = 99
        // Saving goals: PC = 100, Car = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 500, "NL00INGB0123456789", "deposit"));

        // APR 2018
        // Starting balance: 599 - 100 = 499
        // Saving goals: PC = 200, Car = 0, Holiday = 0
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 100, "NL00INGB0123456789", "deposit"));
        RequestHelper.postSavingGoal(sessionID, new SavingGoal(
                "Holiday", 1000, 85, 90));

        // MAY 2018
        // Starting balance: 599 - 100 - 85 = 414
        // Saving goals: PC = 300, Car = 0, Holiday = 85
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 4, "NL00INGB0123456789", "deposit"));

        // JUN 2018
        // Starting balance: 418 - 100 - 85 = 233
        // Saving goals: PC = 400, Car = 0, Holiday = 170
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 1, "NL00INGB0123456789", "deposit"));

        // JUL 2018
        // Starting balance: 234 - 100 - 85 = 49
        // Saving goals: PC = 500, Car = 0, Holiday = 255
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 130, "NL00INGB0123456789", "deposit"));

        // AUG 2018
        // Starting balance: 179 - 90 = 89
        // Saving goals: PC = 590, Car = 0, Holiday = 255
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 1000, "NL00INGB0123456789", "deposit"));

        // SEP 2018
        // Starting balance: 1089 - 250 - 85 = 754
        // Saving goals: PC = 590, Car = 250, Holiday = 340
        localDateTime = localDateTime.plusMonths(1);
        RequestHelper.postTransaction(sessionID, new Transaction(
                IntervalHelper.dateToString(localDateTime), 23, "NL00INGB0123456789", "deposit"));
    }

}
