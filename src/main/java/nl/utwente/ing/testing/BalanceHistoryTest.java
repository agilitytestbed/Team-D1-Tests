package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.IntervalHelper;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class BalanceHistoryTest {

    @Test
    public void testGetBalanceHistoryBasics() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(1);
        localDateTime = localDateTime.minusMonths(24);
        ArrayList<Transaction> transactions = new ArrayList<>();

        localDateTime = localDateTime.plusMonths(5);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMonths(12);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(24));

        for (int i = 0; i <= 3; i++) {
            assertThat((Float) responseList.get(i).get("open"), equalTo((float) 0));
            assertThat((Float) responseList.get(i).get("close"), equalTo((float) 0));
            assertThat((Float) responseList.get(i).get("high"), equalTo((float) 0));
            assertThat((Float) responseList.get(i).get("low"), equalTo((float) 0));
            assertThat((Float) responseList.get(i).get("volume"), equalTo((float) 0));
        }

        assertThat((Float) responseList.get(4).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(4).get("close"), equalTo((float) 700));
        assertThat((Float) responseList.get(4).get("high"), equalTo((float) 1000));
        assertThat((Float) responseList.get(4).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(4).get("volume"), equalTo((float) 1300));

        for (int i = 5; i <= 15; i++) {
            assertThat((Float) responseList.get(i).get("open"), equalTo((float) 700));
            assertThat((Float) responseList.get(i).get("close"), equalTo((float) 700));
            assertThat((Float) responseList.get(i).get("high"), equalTo((float) 700));
            assertThat((Float) responseList.get(i).get("low"), equalTo((float) 700));
            assertThat((Float) responseList.get(i).get("volume"), equalTo((float) 0));
        }

        assertThat((Float) responseList.get(16).get("open"), equalTo((float) 700));
        assertThat((Float) responseList.get(16).get("close"), equalTo((float) 2725));
        assertThat((Float) responseList.get(16).get("high"), equalTo((float) 2725));
        assertThat((Float) responseList.get(16).get("low"), equalTo((float) 200));
        assertThat((Float) responseList.get(16).get("volume"), equalTo((float) 3025));

        for (int i = 17; i <= 23; i++) {
            assertThat((Float) responseList.get(i).get("open"), equalTo((float) 2725));
            assertThat((Float) responseList.get(i).get("close"), equalTo((float) 2725));
            assertThat((Float) responseList.get(i).get("high"), equalTo((float) 2725));
            assertThat((Float) responseList.get(i).get("low"), equalTo((float) 2725));
            assertThat((Float) responseList.get(i).get("volume"), equalTo((float) 0));
        }
    }

    @Test
    public void testEdgeCases() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusHours(1);
        localDateTime = localDateTime.minusMonths(1);
        ArrayList<Transaction> transactions = new ArrayList<>();

        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMonths(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("intervals", 2).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(2));

        assertThat((Float) responseList.get(0).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("close"), equalTo((float) 700));
        assertThat((Float) responseList.get(0).get("high"), equalTo((float) 1000));
        assertThat((Float) responseList.get(0).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("volume"), equalTo((float) 1300));

        assertThat((Float) responseList.get(1).get("open"), equalTo((float) 700));
        assertThat((Float) responseList.get(1).get("close"), equalTo((float) 2725));
        assertThat((Float) responseList.get(1).get("high"), equalTo((float) 2725));
        assertThat((Float) responseList.get(1).get("low"), equalTo((float) 200));
        assertThat((Float) responseList.get(1).get("volume"), equalTo((float) 3025));
    }

    @Test
    public void testCorrectInitialBalance() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusWeeks(1);
        localDateTime = localDateTime.minusMonths(3);
        ArrayList<Transaction> transactions = new ArrayList<>();

        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMonths(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusMinutes(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("intervals", 1).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(1));

        assertThat((Float) responseList.get(0).get("open"), equalTo((float) 2725));
        assertThat((Float) responseList.get(0).get("close"), equalTo((float) 2725));
        assertThat((Float) responseList.get(0).get("high"), equalTo((float) 2725));
        assertThat((Float) responseList.get(0).get("low"), equalTo((float) 2725));
        assertThat((Float) responseList.get(0).get("volume"), equalTo((float) 0));
    }

    @Test
    public void testErrorStatusCodes() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/balance/history").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/balance/history").then().statusCode(401);

        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test invalid input status code
        given().header("X-session-ID", sessionID).
                queryParam("interval", "infinity").
                get(Constants.PREFIX + "/balance/history").then().statusCode(405);
    }

    @Test
    public void testGetBalanceHistoryHours() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(1);
        localDateTime = localDateTime.minusHours(4);
        ArrayList<Transaction> transactions = new ArrayList<>();

        localDateTime = localDateTime.plusHours(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusHours(2);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "hour").queryParam("intervals", 5).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();

        this.reusableCandlestickTest(responseString);
    }

    @Test
    public void testGetBalanceHistoryDays() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(1);
        localDateTime = localDateTime.minusDays(4);
        ArrayList<Transaction> transactions = new ArrayList<>();

        localDateTime = localDateTime.plusDays(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusDays(2);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "day").queryParam("intervals", 5).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();

        this.reusableCandlestickTest(responseString);
    }

    @Test
    public void testGetBalanceHistoryWeeks() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(1);
        localDateTime = localDateTime.minusWeeks(4);
        ArrayList<Transaction> transactions = new ArrayList<>();

        localDateTime = localDateTime.plusWeeks(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusWeeks(2);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "week").queryParam("intervals", 5).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();

        this.reusableCandlestickTest(responseString);
    }

    @Test
    public void testGetBalanceHistoryMonth() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(1);
        localDateTime = localDateTime.minusMonths(4);
        ArrayList<Transaction> transactions = new ArrayList<>();

        localDateTime = localDateTime.plusMonths(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusMonths(2);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "month").queryParam("intervals", 5).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();

        this.reusableCandlestickTest(responseString);
    }

    @Test
    public void testGetBalanceHistoryYear() {
        // Acquire legal session ID
        String sessionID = RequestHelper.getNewSessionID();

        // Test valid application workflow
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(1);
        localDateTime = localDateTime.minusYears(4);
        ArrayList<Transaction> transactions = new ArrayList<>();

        localDateTime = localDateTime.plusYears(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 1000,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 300,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusYears(2);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 500,
                "NL42INGB0123456789", "withdrawal"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 2500,
                "NL42INGB0123456789", "deposit"));
        localDateTime = localDateTime.plusSeconds(1);
        transactions.add(new Transaction(IntervalHelper.dateToString(localDateTime), 25,
                "NL42INGB0123456789", "deposit"));

        for (Transaction transaction : transactions) {
            RequestHelper.postTransaction(sessionID, transaction);
        }

        String responseString = given().header("X-session-ID", sessionID).
                queryParam("interval", "year").queryParam("intervals", 5).
                get(Constants.PREFIX + "/balance/history").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();

        this.reusableCandlestickTest(responseString);
    }

    private void reusableCandlestickTest(String responseString) {

        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(5));

        assertThat((Float) responseList.get(0).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("close"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("high"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(0).get("volume"), equalTo((float) 0));

        assertThat((Float) responseList.get(1).get("open"), equalTo((float) 0));
        assertThat((Float) responseList.get(1).get("close"), equalTo((float) 700));
        assertThat((Float) responseList.get(1).get("high"), equalTo((float) 1000));
        assertThat((Float) responseList.get(1).get("low"), equalTo((float) 0));
        assertThat((Float) responseList.get(1).get("volume"), equalTo((float) 1300));

        assertThat((Float) responseList.get(2).get("open"), equalTo((float) 700));
        assertThat((Float) responseList.get(2).get("close"), equalTo((float) 700));
        assertThat((Float) responseList.get(2).get("high"), equalTo((float) 700));
        assertThat((Float) responseList.get(2).get("low"), equalTo((float) 700));
        assertThat((Float) responseList.get(2).get("volume"), equalTo((float) 0));

        assertThat((Float) responseList.get(3).get("open"), equalTo((float) 700));
        assertThat((Float) responseList.get(3).get("close"), equalTo((float) 2725));
        assertThat((Float) responseList.get(3).get("high"), equalTo((float) 2725));
        assertThat((Float) responseList.get(3).get("low"), equalTo((float) 200));
        assertThat((Float) responseList.get(3).get("volume"), equalTo((float) 3025));

        assertThat((Float) responseList.get(4).get("open"), equalTo((float) 2725));
        assertThat((Float) responseList.get(4).get("close"), equalTo((float) 2725));
        assertThat((Float) responseList.get(4).get("high"), equalTo((float) 2725));
        assertThat((Float) responseList.get(4).get("low"), equalTo((float) 2725));
        assertThat((Float) responseList.get(4).get("volume"), equalTo((float) 0));
    }

}
