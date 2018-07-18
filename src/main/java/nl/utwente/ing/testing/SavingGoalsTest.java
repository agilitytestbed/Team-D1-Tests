package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.CategoryRule;
import nl.utwente.ing.testing.bean.SavingGoal;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.Test;

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

//        ArrayList<Transaction> transactions = new ArrayList<>();
//        transactions.add(new Transaction("2018-07-16T07:30:18.028Z",
//                1, "NL34INGB9012345678", "deposit"));
//        transactions.add(new Transaction("2018-07-18T07:30:18.028Z",
//                2, "NL34INGB9012345678", "deposit"));
//        transactions.add(new Transaction("2018-07-17T07:30:18.028Z",
//                3, "NL34INGB9012345678", "deposit"));
//
//        for (Transaction transaction : transactions) {
//            RequestHelper.postTransaction(sessionID, transaction);
//        }

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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
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
                then().contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(3));
        assertThat(responseList.get(0).get("id"), equalTo((int) pcSavingGoalID));
        assertThat(responseList.get(0).get("balance"), equalTo((float) 590));
        assertThat(responseList.get(1).get("id"), equalTo((int) carSavingGoalID));
        assertThat(responseList.get(1).get("balance"), equalTo((float) 250));
        assertThat(responseList.get(2).get("id"), equalTo((int) holidaySavingGoalID));
        assertThat(responseList.get(2).get("balance"), equalTo((float) 340));
    }

}