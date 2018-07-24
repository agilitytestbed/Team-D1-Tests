package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.PaymentRequest;
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
                "2017-01-01T00:00:00.000Z", 5, 1, false));
        paymentRequests.add(new PaymentRequest("Books",
                "2017-01-02T00:00:00.000Z", 75, 2, false));
        paymentRequests.add(new PaymentRequest("Beer",
                "2017-01-03T00:00:00.000Z", 4, 4, false));
        paymentRequests.add(new PaymentRequest("Stuff",
                "2017-01-04T00:00:00.000Z", 15, 3, false));
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
            assertThat(responseList.get(i).get("filled"), equalTo(paymentRequests.get(i).getFilled()));
        }
    }

    @Test
    public void testPostPaymentRequestBasic() {
        String sessionID = RequestHelper.getNewSessionID();
        PaymentRequest paymentRequest = new PaymentRequest("Food",
                "2017-01-01T00:00:00.000Z", 5, 1, false);

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
                body("filled", equalTo(false));

        // Test invalid input status code
        paymentRequest.setNumber_of_requests(-1);
        given().contentType("application/json").
                body(paymentRequest).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/paymentRequests").then().statusCode(405);
    }
}
