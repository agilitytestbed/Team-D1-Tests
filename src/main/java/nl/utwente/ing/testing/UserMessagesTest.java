package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.PaymentRequest;
import nl.utwente.ing.testing.bean.SavingGoal;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.bean.UserMessage;
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

public class UserMessagesTest {

    private static final boolean CHECK_MESSAGES = true;

    @Test
    public void testSimpleBalanceBelowZero() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 50,
                "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-08-06T15:44:56.789Z", false, "warning"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 100,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testSimpleBalanceNewHigh() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-12-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Balance reach new high.",
                "2018-12-05T15:44:56.789Z", false, "info"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testSimplePaymentRequestFilled() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        long paymentRequestID = RequestHelper.postPaymentRequest(sessionID, new PaymentRequest("Dinner",
                "2018-08-19T15:44:56.789Z", 10, 3));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-08T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Payment request filled: Dinner (ID = " + paymentRequestID + ").",
                "2018-08-08T15:44:56.789Z", false, "info"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-09T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testSimplePaymentRequestNotFilled() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        long paymentRequestID = RequestHelper.postPaymentRequest(sessionID, new PaymentRequest("Dinner",
                "2018-08-07T16:44:56.789Z", 10, 3));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-08T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Payment request not filled: Dinner (ID = " + paymentRequestID + ").",
                "2018-08-08T15:44:56.789Z", false, "warning"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-09T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testSavingGoalReached() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        long savingGoalID = RequestHelper.postSavingGoal(sessionID, new SavingGoal("Holiday",
                400, 400, 400));
        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 5000,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-09-05T15:44:56.789Z", 1,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Saving goal reached: Holiday (ID = " + savingGoalID + ").",
                "2018-09-05T15:44:56.789Z", false, "info"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testSimpleMarkAsRead() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 50,
                "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-08-06T15:44:56.789Z", false, "warning"));
        verifyUserMessages(sessionID, expectedMessages);

        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/messages").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        long messageID = Long.parseLong(responseList.get(0).get("id").toString());
        given().header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/messages/" + messageID).then().statusCode(200);
        expectedMessages.remove(0);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 100,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testInvalidGetMessages() {
        when().get(Constants.PREFIX + "/messages").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/messages").then().statusCode(401);
    }

    @Test
    public void testInvalidPutMessages() {
        when().put(Constants.PREFIX + "/messages/213728").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").
                put(Constants.PREFIX + "/messages/213728").then().statusCode(401);

        String sessionID = RequestHelper.getNewSessionID();
        given().header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/messages/213728").then().statusCode(404);
    }

    @Test
    public void testBalanceStayBelowZero() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-08-05T15:44:56.789Z", false, "warning"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 50,
                "NL42INGB0123456789", "withdrawal"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 100,
                "NL42INGB0123456789", "withdrawal"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceBelowZeroRepeatedly() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-08-05T15:44:56.789Z", false, "warning"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 50,
                "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-08-06T15:44:56.789Z", false, "warning"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 50,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 100,
                "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-08-07T15:44:56.789Z", false, "warning"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceExactlyZero() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 10,
                "NL42INGB0123456789", "withdrawal"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 100,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceNewHighBeforeThreeMonths() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-10-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceNewHighPreviousMessageUnread() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-12-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Balance reach new high.",
                "2018-12-05T15:44:56.789Z", false, "info"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2019-01-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceNewHighPreviousMessageRead() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-12-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Balance reach new high.",
                "2018-12-05T15:44:56.789Z", false, "info"));
        verifyUserMessages(sessionID, expectedMessages);

        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/messages").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        long messageID = Long.parseLong(responseList.get(0).get("id").toString());
        given().header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/messages/" + messageID).then().statusCode(200);
        expectedMessages.remove(0);

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2019-01-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Balance reach new high.",
                "2019-01-05T15:44:56.789Z", false, "info"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceNewHighExactlyOldHigh() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-12-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Balance reach new high.",
                "2018-12-05T15:44:56.789Z", false, "info"));
        verifyUserMessages(sessionID, expectedMessages);

        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/messages").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        long messageID = Long.parseLong(responseList.get(0).get("id").toString());
        given().header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/messages/" + messageID).then().statusCode(200);
        expectedMessages.remove(0);
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-12-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "withdrawal"));
        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2019-01-05T15:44:56.789Z", 300,
                "NL42INGB0123456789", "deposit"));
        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testMultiplePaymentRequestsFilled() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        long paymentRequestID1 = RequestHelper.postPaymentRequest(sessionID, new PaymentRequest("Dinner",
                "2018-08-19T15:44:56.789Z", 10, 3));
        long paymentRequestID2 = RequestHelper.postPaymentRequest(sessionID, new PaymentRequest("Books",
                "2018-08-19T15:44:56.789Z", 10, 1));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-08T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Payment request filled: Dinner (ID = " + paymentRequestID1 + ").",
                "2018-08-08T15:44:56.789Z", false, "info"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-09T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Payment request filled: Books (ID = " + paymentRequestID2 + ").",
                "2018-08-09T15:44:56.789Z", false, "info"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testMultiplePaymentRequestsNotFilled() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        long paymentRequestID1 = RequestHelper.postPaymentRequest(sessionID, new PaymentRequest("Dinner",
                "2018-08-07T16:44:56.789Z", 10, 3));
        long paymentRequestID2 = RequestHelper.postPaymentRequest(sessionID, new PaymentRequest("Books",
                "2018-08-07T16:44:56.789Z", 10, 1));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-06T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-08T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Payment request not filled: Dinner (ID = " + paymentRequestID1 + ").",
                "2018-08-08T15:44:56.789Z", false, "warning"));
        expectedMessages.add(new UserMessage("Payment request not filled: Books (ID = " + paymentRequestID2 + ").",
                "2018-08-08T15:44:56.789Z", false, "warning"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-09T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testMultipleSavingGoalsReached() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        long savingGoalID1 = RequestHelper.postSavingGoal(sessionID, new SavingGoal("Holiday",
                400, 400, 400));
        long savingGoalID2 = RequestHelper.postSavingGoal(sessionID, new SavingGoal("Entertainment",
                400, 400, 400));
        long savingGoalID3 = RequestHelper.postSavingGoal(sessionID, new SavingGoal("Study",
                600, 400, 400));
        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 5000,
                "NL42INGB0123456789", "deposit"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-09-05T15:44:56.789Z", 1,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Saving goal reached: Holiday (ID = " + savingGoalID1 + ").",
                "2018-09-05T15:44:56.789Z", false, "info"));
        expectedMessages.add(new UserMessage("Saving goal reached: Entertainment (ID = " + savingGoalID2 + ").",
                "2018-09-05T15:44:56.789Z", false, "info"));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-10-05T15:44:56.789Z", 1,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Saving goal reached: Study (ID = " + savingGoalID3 + ").",
                "2018-10-05T15:44:56.789Z", false, "info"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testBalanceBelowZeroBySavingGoal() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-05T15:44:56.789Z", 10,
                "NL42INGB0123456789", "deposit"));
        RequestHelper.postSavingGoal(sessionID, new SavingGoal("Clothing",
                1000, 100, 0));

        verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-09-05T15:44:56.789Z", 20,
                "NL42INGB0123456789", "deposit"));
        expectedMessages.add(new UserMessage("Balance drop below zero.",
                "2018-09-05T15:44:56.789Z", false, "warning"));

        verifyUserMessages(sessionID, expectedMessages);
    }

    protected static void verifyUserMessages(String sessionID, ArrayList<UserMessage> expectedMessages) {
        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/messages").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(expectedMessages.size()));
        for (int i = 0; i < expectedMessages.size(); i++) {
            UserMessage userMessage = expectedMessages.get(i);
            assertThat(responseList.get(i), hasKey("id"));
            assertThat(responseList.get(i).get("date"), equalTo(userMessage.getDate()));
            assertThat(responseList.get(i).get("read"), equalTo(userMessage.getRead()));
            assertThat(responseList.get(i).get("type"), equalTo(userMessage.getType()));
            if (CHECK_MESSAGES) {
                assertThat(responseList.get(i).get("message"), equalTo(userMessage.getMessage()));
            } else {
                assertThat(responseList.get(i), hasKey("message"));
            }
        }
    }
}
