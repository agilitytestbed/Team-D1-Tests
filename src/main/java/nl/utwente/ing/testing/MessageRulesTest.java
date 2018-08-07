package nl.utwente.ing.testing;

import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.MessageRule;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class MessageRulesTest {

    @Test
    public void testPostMessageRuleBasics() {
        String sessionID = RequestHelper.getNewSessionID();

        long categoryID = RequestHelper.postCategory(sessionID, new Category("Groceries"));
        MessageRule messageRule = new MessageRule(categoryID, "warning", 10);

        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID)).
                body("type", equalTo("warning")).
                body("value", equalTo((float) 10));
    }

    @Test
    public void testInvalidPostMessageRule() {
        String sessionID = RequestHelper.getNewSessionID();

        long categoryID = RequestHelper.postCategory(sessionID, new Category("Groceries"));
        MessageRule messageRule = new MessageRule(categoryID, "warning", 10);

        // Test invalid session ID status code
        given().contentType("application/json").
                body(messageRule).
                post(Constants.PREFIX + "/messageRules").then().statusCode(401);
        given().contentType("application/json").
                body(messageRule).header("X-session-ID", "A1B2C3D4E5").
                post(Constants.PREFIX + "/messageRules").then().statusCode(401);

        // Test invalid input status code
        messageRule.setType("random");
        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").then().statusCode(405);

        messageRule.setType("warning");
        messageRule.setCategory_id(-1);
        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").then().statusCode(405);

        messageRule.setCategory_id(categoryID);
        messageRule.setValue(-1);
        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").then().statusCode(405);
    }

}
