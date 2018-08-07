package nl.utwente.ing.testing;

import nl.utwente.ing.testing.bean.*;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.Test;

import java.util.ArrayList;

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

    @Test
    public void testInfoCategoryLimit() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        long categoryID = RequestHelper.postCategory(sessionID, new Category("Groceries"));
        MessageRule messageRule = new MessageRule(categoryID, "info", 10);

        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID)).
                body("type", equalTo("info")).
                body("value", equalTo((float) 10));

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("Food", "", "withdrawal",
                categoryID, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:00.000Z", 250,
                "Free money", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:31:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:32:00.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Groceries (ID = " + categoryID + ").",
                "2018-08-07T11:32:00.000Z", false, "info"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testWarningCategoryLimit() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        long categoryID = RequestHelper.postCategory(sessionID, new Category("Rent"));
        MessageRule messageRule = new MessageRule(categoryID, "warning", 250);

        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID)).
                body("type", equalTo("warning")).
                body("value", equalTo((float) 250));

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("House", "", "",
                categoryID, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:00.000Z", 1000,
                "House", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:31:00.000Z", 250,
                "House", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:32:00.000Z", 250,
                "House", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Rent (ID = " + categoryID + ").",
                "2018-08-07T11:32:00.000Z", false, "warning"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testMultipleDifferentCategoryLimitsReached() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        long categoryID1 = RequestHelper.postCategory(sessionID, new Category("Groceries"));
        long categoryID2 = RequestHelper.postCategory(sessionID, new Category("Rent"));
        MessageRule messageRule1 = new MessageRule(categoryID1, "warning", 10);
        MessageRule messageRule2 = new MessageRule(categoryID2, "info", 250);

        given().contentType("application/json").
                body(messageRule1).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID1)).
                body("type", equalTo("warning")).
                body("value", equalTo((float) 10));
        given().contentType("application/json").
                body(messageRule2).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID2)).
                body("type", equalTo("info")).
                body("value", equalTo((float) 250));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("Food", "", "withdrawal",
                categoryID1, true));
        RequestHelper.postCategoryRule(sessionID, new CategoryRule("House", "", "",
                categoryID2, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:27:00.000Z", 250,
                "Free money", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:28:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:29:00.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Groceries (ID = " + categoryID1 + ").",
                "2018-08-07T11:29:00.000Z", false, "warning"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:00.000Z", 1000,
                "House", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:31:00.000Z", 250,
                "House", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:32:00.000Z", 250,
                "House", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Rent (ID = " + categoryID2 + ").",
                "2018-08-07T11:32:00.000Z", false, "info"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testCategoryLimitsReachedMultipleTimesWithinThirtyDays() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        long categoryID1 = RequestHelper.postCategory(sessionID, new Category("Groceries"));
        MessageRule messageRule1 = new MessageRule(categoryID1, "warning", 10);

        given().contentType("application/json").
                body(messageRule1).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID1)).
                body("type", equalTo("warning")).
                body("value", equalTo((float) 10));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("Food", "", "withdrawal",
                categoryID1, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:27:00.000Z", 250,
                "Free money", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:28:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:29:00.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Groceries (ID = " + categoryID1 + ").",
                "2018-08-07T11:29:00.000Z", false, "warning"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:31:00.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testCategoryLimitsReachedMultipleTimesOutsideThirtyDays() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        long categoryID1 = RequestHelper.postCategory(sessionID, new Category("Groceries"));
        MessageRule messageRule1 = new MessageRule(categoryID1, "warning", 10);

        given().contentType("application/json").
                body(messageRule1).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID1)).
                body("type", equalTo("warning")).
                body("value", equalTo((float) 10));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("Food", "", "withdrawal",
                categoryID1, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:27:00.000Z", 250,
                "Free money", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:28:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:29:00.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Groceries (ID = " + categoryID1 + ").",
                "2018-08-07T11:29:00.000Z", false, "warning"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-09-07T11:30:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-09-07T11:31:00.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));
        expectedMessages.add(new UserMessage("Category limit reached: Groceries (ID = " + categoryID1 + ").",
                "2018-09-07T11:31:00.000Z", false, "warning"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testCategoryLimitNotReachedByWrongOrder() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

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

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("Food", "", "withdrawal",
                categoryID, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:00.000Z", 250,
                "Free money", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:31:00.000Z", 9,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:30.000Z", 2,
                "Food", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:32:00.000Z", 15,
                "Book", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

    @Test
    public void testCategoryLimitExactlyNotReached() {
        String sessionID = RequestHelper.getNewSessionID();
        ArrayList<UserMessage> expectedMessages = new ArrayList<>();

        long categoryID = RequestHelper.postCategory(sessionID, new Category("Rent"));
        MessageRule messageRule = new MessageRule(categoryID, "info", 250);

        given().contentType("application/json").
                body(messageRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/messageRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("category_id", equalTo((int) categoryID)).
                body("type", equalTo("info")).
                body("value", equalTo((float) 250));

        RequestHelper.postCategoryRule(sessionID, new CategoryRule("House", "", "withdrawal",
                categoryID, true));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:30:00.000Z", 1000,
                "Free money", "NL42INGB0123456789", "deposit"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);

        RequestHelper.postTransaction(sessionID, new Transaction("2018-08-07T11:31:00.000Z", 250,
                "House", "NL42INGB0123456789", "withdrawal"));

        UserMessagesTest.verifyUserMessages(sessionID, expectedMessages);
    }

}
