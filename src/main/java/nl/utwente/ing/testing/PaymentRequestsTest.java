package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.PaymentRequest;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.path.json.JsonPath.from;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class PaymentRequestsTest {

    @Test
    public void testGetPaymentRequests() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/paymentRequests").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/paymentRequests").then().statusCode(401);

        // Test responses and status codes
        String newSessionID = RequestHelper.getNewSessionID();
        ArrayList<PaymentRequest> paymentRequests = new ArrayList<>();
        paymentRequests.add(new PaymentRequest("Food",
                "2017-01-01T00:00:00.000Z", 5, 1));
        paymentRequests.add(new PaymentRequest("Books",
                "2017-01-02T00:00:00.000Z", 75, 2));
        paymentRequests.add(new PaymentRequest("Beer",
                "2017-01-03T00:00:00.000Z", 4, 4));
        paymentRequests.add(new PaymentRequest("Stuff",
                "2017-01-04T00:00:00.000Z", 15, 3));
        for (PaymentRequest paymentRequest : paymentRequests) {
            RequestHelper.postPaymentRequest(newSessionID, paymentRequest);
        }

        String responseString = given().header("X-session-ID", newSessionID).
                get(Constants.PREFIX + "/paymentRequests").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(4));
        for (int i = 0; i <  paymentRequests.size(); i++) {
            assertThat(responseList.get(i), hasKey("id"));
            assertThat(responseList.get(i).get("description"), equalTo(paymentRequests.get(i).getDescription()));
            assertThat(responseList.get(i).get("due_date"), equalTo(paymentRequests.get(i).getDue_date()));
            assertThat(responseList.get(i).get("amount"), equalTo(paymentRequests.get(i).getAmount()));
            assertThat(responseList.get(i).get("number_of_requests"), equalTo((int) paymentRequests.get(i).getNumber_of_requests()));
            assertThat(responseList.get(i).get("filled"), equalTo(false));
            assertThat(responseList.get(i), hasKey("transactions"));
        }
    }

    @Test
    public void testPostPaymentRequestBasic() {
        String sessionID = RequestHelper.getNewSessionID();
        PaymentRequest paymentRequest = new PaymentRequest("Food",
                "2017-01-01T00:00:00.000Z", 5, 1);

        // Test invalid session ID status code
        given().contentType("application/json").
                body(paymentRequest).
                post(Constants.PREFIX + "/paymentRequests").then().statusCode(401);
        given().contentType("application/json").
                body(paymentRequest).header("X-session-ID", "A1B2C3D4E5").
                post(Constants.PREFIX + "/paymentRequests").then().statusCode(401);

        // Test valid paymentRequest post response and status code
        given().contentType("application/json").
                body(paymentRequest).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/paymentRequests").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("description", equalTo("Food")).
                body("due_date", equalTo("2017-01-01T00:00:00.000Z")).
                body("amount", equalTo((float) 5)).
                body("number_of_requests", equalTo(1)).
                body("filled", equalTo(false)).
                body("$", hasKey("transactions"));

        // Test invalid input status code
        paymentRequest.setNumber_of_requests(-1);
        given().contentType("application/json").
                body(paymentRequest).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/paymentRequests").then().statusCode(405);
    }

    @Test
    public void testSinglePaymentRequestTransactions() {
        ArrayList<Transaction> answeringTransactions = new ArrayList<>();
        String sessionID = RequestHelper.getNewSessionID();

        PaymentRequest paymentRequest = new PaymentRequest("Dinner",
                "2018-09-01T00:00:00.000Z", 10, 4);
        long paymentRequestID = RequestHelper.postPaymentRequest(sessionID, paymentRequest);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t1 = new Transaction("2018-07-21T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t1);
        answeringTransactions.add(t1);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t2 = new Transaction("2018-07-22T12:34:56.789Z",
                (float) 10.5, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t3 = new Transaction("2018-07-23T12:34:56.789Z",
                10, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t3);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t4 = new Transaction("2018-07-24T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t4);
        answeringTransactions.add(t4);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t5 = new Transaction("2018-07-25T12:34:56.789Z",
                9, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t5);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t6 = new Transaction("2018-07-26T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t6);
        answeringTransactions.add(t6);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t7 = new Transaction("2018-07-27T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t7);
        answeringTransactions.add(t7);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t8 = new Transaction("2018-07-28T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t8);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);
    }

    @Test
    public void testMultipleSingleAmountPaymentRequestsTransactions() {
        ArrayList<Transaction> answeringTransactions1 = new ArrayList<>();
        ArrayList<Transaction> answeringTransactions2 = new ArrayList<>();
        String sessionID = RequestHelper.getNewSessionID();

        PaymentRequest paymentRequest1 = new PaymentRequest("Dinner",
                "2018-09-01T00:00:00.000Z", 10, 1);
        long paymentRequestID1 = RequestHelper.postPaymentRequest(sessionID, paymentRequest1);

        PaymentRequest paymentRequest2 = new PaymentRequest("Party",
                "2018-09-01T00:00:00.000Z", 10, 3);
        long paymentRequestID2 = RequestHelper.postPaymentRequest(sessionID, paymentRequest2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t1 = new Transaction("2018-07-21T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t1);
        answeringTransactions1.add(t1);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t2 = new Transaction("2018-07-22T12:34:56.789Z",
                (float) 10.5, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t3 = new Transaction("2018-07-23T12:34:56.789Z",
                10, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t3);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t4 = new Transaction("2018-07-24T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t4);
        answeringTransactions2.add(t4);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t5 = new Transaction("2018-07-25T12:34:56.789Z",
                9, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t5);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t6 = new Transaction("2018-07-26T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t6);
        answeringTransactions2.add(t6);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t7 = new Transaction("2018-07-27T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t7);
        answeringTransactions2.add(t7);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t8 = new Transaction("2018-07-28T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t8);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);
    }

    @Test
    public void testMultipleDifferentAmountPaymentRequestsTransactions() {
        ArrayList<Transaction> answeringTransactions1 = new ArrayList<>();
        ArrayList<Transaction> answeringTransactions2 = new ArrayList<>();
        String sessionID = RequestHelper.getNewSessionID();

        PaymentRequest paymentRequest1 = new PaymentRequest("Dinner",
                "2018-09-01T00:00:00.000Z", 10, 2);
        long paymentRequestID1 = RequestHelper.postPaymentRequest(sessionID, paymentRequest1);

        PaymentRequest paymentRequest2 = new PaymentRequest("Party",
                "2018-09-01T00:00:00.000Z", 15, 2);
        long paymentRequestID2 = RequestHelper.postPaymentRequest(sessionID, paymentRequest2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t1 = new Transaction("2018-07-21T12:34:56.789Z",
                15, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t1);
        answeringTransactions2.add(t1);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t2 = new Transaction("2018-07-22T12:34:56.789Z",
                (float) 10.5, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t3 = new Transaction("2018-07-23T12:34:56.789Z",
                10, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t3);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t4 = new Transaction("2018-07-24T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t4);
        answeringTransactions1.add(t4);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t5 = new Transaction("2018-07-25T12:34:56.789Z",
                9, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t5);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t6 = new Transaction("2018-07-26T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t6);
        answeringTransactions1.add(t6);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t7 = new Transaction("2018-07-27T12:34:56.789Z",
                15, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t7);
        answeringTransactions2.add(t7);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t8 = new Transaction("2018-07-28T12:34:56.789Z",
                15, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t8);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);

        Transaction t9 = new Transaction("2018-07-29T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t9);

        verifyPaymentRequestResponse(sessionID, paymentRequestID1, paymentRequest1, answeringTransactions1);
        verifyPaymentRequestResponse(sessionID, paymentRequestID2, paymentRequest2, answeringTransactions2);
    }

    @Test
    public void testPaymentRequestTransactionsAfterDueDateFilled() {
        ArrayList<Transaction> answeringTransactions = new ArrayList<>();
        String sessionID = RequestHelper.getNewSessionID();

        PaymentRequest paymentRequest = new PaymentRequest("Dinner",
                "2018-07-28T12:00:00.000Z", 10, 4);
        long paymentRequestID = RequestHelper.postPaymentRequest(sessionID, paymentRequest);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t1 = new Transaction("2018-07-21T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t1);
        answeringTransactions.add(t1);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t2 = new Transaction("2018-07-22T12:34:56.789Z",
                (float) 10.5, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t3 = new Transaction("2018-07-23T12:34:56.789Z",
                10, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t3);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t4 = new Transaction("2018-07-24T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t4);
        answeringTransactions.add(t4);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t5 = new Transaction("2018-07-25T12:34:56.789Z",
                9, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t5);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t6 = new Transaction("2018-07-26T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t6);
        answeringTransactions.add(t6);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t7 = new Transaction("2018-07-27T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t7);
        answeringTransactions.add(t7);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t8 = new Transaction("2018-07-28T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t8);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);
    }

    @Test
    public void testPaymentRequestTransactionsAfterDueDateNotFilled() {
        ArrayList<Transaction> answeringTransactions = new ArrayList<>();
        String sessionID = RequestHelper.getNewSessionID();

        PaymentRequest paymentRequest = new PaymentRequest("Dinner",
                "2018-07-27T12:00:00.000Z", 10, 4);
        long paymentRequestID = RequestHelper.postPaymentRequest(sessionID, paymentRequest);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t1 = new Transaction("2018-07-21T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t1);
        answeringTransactions.add(t1);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t2 = new Transaction("2018-07-22T12:34:56.789Z",
                (float) 10.5, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t2);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t3 = new Transaction("2018-07-23T12:34:56.789Z",
                10, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t3);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t4 = new Transaction("2018-07-24T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t4);
        answeringTransactions.add(t4);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t5 = new Transaction("2018-07-25T12:34:56.789Z",
                9, "NL45INGB0123456789", "withdrawal");
        RequestHelper.postTransaction(sessionID, t5);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t6 = new Transaction("2018-07-26T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t6);
        answeringTransactions.add(t6);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t7 = new Transaction("2018-07-27T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t7);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t8 = new Transaction("2018-07-28T12:34:56.789Z",
                10, "NL45INGB0123456789", "deposit");
        RequestHelper.postTransaction(sessionID, t8);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);
    }

    @Test
    public void testCategoryInPaymentRequestTransaction() {
        ArrayList<Transaction> answeringTransactions = new ArrayList<>();
        String sessionID = RequestHelper.getNewSessionID();

        PaymentRequest paymentRequest = new PaymentRequest("Paint",
                "2018-09-01T00:00:00.000Z", 95, 1);
        long paymentRequestID = RequestHelper.postPaymentRequest(sessionID, paymentRequest);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        Transaction t1 = new Transaction("2018-07-21T12:34:56.789Z",
                95, "NL45INGB0123456789", "deposit");
        long transactionID = RequestHelper.postTransaction(sessionID, t1);
        answeringTransactions.add(t1);

        verifyPaymentRequestResponse(sessionID, paymentRequestID, paymentRequest, answeringTransactions);

        long categoryID = RequestHelper.postCategory(sessionID, new Category("Creativity"));
        RequestHelper.assignCategoryToTransaction(sessionID, transactionID, categoryID);

        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/paymentRequests").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        assertTrue(responseString.contains("\"category\":{"));
        assertTrue(responseString.contains("\"name\":\"Creativity\""));
    }

    private void verifyPaymentRequestResponse(String sessionID, long paymentRequestID, PaymentRequest paymentRequest,
                                             ArrayList<Transaction> answeringTransactions) {
        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/paymentRequests").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        boolean shouldBeFilled = answeringTransactions.size() == paymentRequest.getNumber_of_requests();
        boolean shouldFail = true;
        for (int i = 0; i < responseList.size(); i++) {
            if (responseList.get(i).get("id").toString().equals(Long.toString(paymentRequestID))) {
                assertThat(responseList.get(i).get("id"), equalTo((int) paymentRequestID));
                assertThat(responseList.get(i).get("description"), equalTo(paymentRequest.getDescription()));
                assertThat(responseList.get(i).get("due_date"), equalTo(paymentRequest.getDue_date()));
                assertThat(responseList.get(i).get("amount"), equalTo(paymentRequest.getAmount()));
                assertThat(responseList.get(i).get("number_of_requests"), equalTo((int) paymentRequest.getNumber_of_requests()));
                assertThat(responseList.get(i).get("filled"), equalTo(shouldBeFilled));
                assertThat(responseList.get(i), hasKey("transactions"));

                String innerResponseString = responseList.get(i).get("transactions").toString();
                assertEquals(answeringTransactions.size(), StringUtils.countMatches(innerResponseString, '{'));
                for (Transaction transaction : answeringTransactions) {
                    assertTrue(innerResponseString.contains(transaction.getDate()));
                }
                shouldFail = false;
            }
        }

        if (shouldFail) {
            fail();
        }
    }

}
