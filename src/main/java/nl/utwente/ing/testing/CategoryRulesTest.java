package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.CategoryRule;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.Helper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class CategoryRulesTest {

    @Test
    public void testGetCategoryRules() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/categoryRules").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/categoryRules").then().statusCode(401);

        // Test responses and status codes
        String newSessionID = Helper.getNewSessionID();
        ArrayList<CategoryRule> categoryRules = new ArrayList<>();
        categoryRules.add(new CategoryRule("strawberry", "NL66ABNA0123456789", "deposit", 1, false));
        categoryRules.add(new CategoryRule("blueberry", "NL67ABNA0123456789", "deposit", 2, false));
        categoryRules.add(new CategoryRule("raspberry", "NL68ABNA0123456789", "deposit", 3, false));
        categoryRules.add(new CategoryRule("", "NL69ABNA0123456789", "withdrawal", 4, true));
        for (CategoryRule categoryRule : categoryRules) {
            Helper.postCategoryRule(newSessionID, categoryRule);
        }

        String responseString = given().header("X-session-ID", newSessionID).
                get(Constants.PREFIX + "/categoryRules").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(4));
        for (int i = 0; i <  categoryRules.size(); i++) {
            assertThat(responseList.get(i), hasKey("id"));
            assertThat((String) responseList.get(i).get("description"), equalTo(categoryRules.get(i).getDescription()));
            assertThat((String) responseList.get(i).get("iBAN"), equalTo(categoryRules.get(i).getiBAN()));
            assertThat((String) responseList.get(i).get("type"), equalTo(categoryRules.get(i).getType()));
            assertThat(new Long((Integer) responseList.get(i).get("category_id")), equalTo(categoryRules.get(i).getCategory_id()));
            assertThat((Boolean) responseList.get(i).get("applyOnHistory"), equalTo(categoryRules.get(i).getApplyOnHistory()));
        }
    }

    @Test
    public void testPostCategoryRuleBasic() {
        String sessionID = Helper.getNewSessionID();

        CategoryRule categoryRule =
                new CategoryRule("description", "iban", "withdrawal", 1, false);

        // Test invalid session ID status code
        given().contentType("application/json").
                body(categoryRule).
                post(Constants.PREFIX + "/categoryRules").then().statusCode(401);
        given().contentType("application/json").
                body(categoryRule).header("X-session-ID", "A1B2C3D4E5").
                post(Constants.PREFIX + "/categoryRules").then().statusCode(401);

        // Test valid categoryRule post response and status code
        given().contentType("application/json").
                body(categoryRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categoryRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("description", equalTo("description")).
                body("iBAN", equalTo("iban")).
                body("type", equalTo("withdrawal")).
                body("category_id", equalTo(1)).
                body("applyOnHistory", equalTo(false));
        categoryRule.setDescription("");
        given().contentType("application/json").
                body(categoryRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categoryRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("description", equalTo("")).
                body("iBAN", equalTo("iban")).
                body("type", equalTo("withdrawal")).
                body("category_id", equalTo(1)).
                body("applyOnHistory", equalTo(false));
        categoryRule.setiBAN("");
        given().contentType("application/json").
                body(categoryRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categoryRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("description", equalTo("")).
                body("iBAN", equalTo("")).
                body("type", equalTo("withdrawal")).
                body("category_id", equalTo(1)).
                body("applyOnHistory", equalTo(false));
        categoryRule.setType("");
        categoryRule.setDescription("xxx");
        categoryRule.setApplyOnHistory(true);
        given().contentType("application/json").
                body(categoryRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categoryRules").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("description", equalTo("xxx")).
                body("iBAN", equalTo("")).
                body("type", equalTo("")).
                body("category_id", equalTo(1)).
                body("applyOnHistory", equalTo(true));

        // Test invalid input status code
        categoryRule.setType("xxx");
        given().contentType("application/json").
                body(categoryRule).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categoryRules").then().statusCode(405);
    }

    @Test
    public void testPostCategoryRuleNewTransaction() {
        String sessionID = Helper.getNewSessionID();

        // Create categories
        ArrayList<Category> categories = new ArrayList<>();
        categories.add(new Category("Apple"));
        categories.add(new Category("Pear"));
        categories.add(new Category("Strawberry"));

        ArrayList<Long> categoryIDs = new ArrayList<>();
        for (Category category : categories) {
            categoryIDs.add(Helper.postCategory(sessionID, category));
        }

        // Create categoryRules
        Helper.postCategoryRule(sessionID,
                new CategoryRule("", "", "deposit", 83818238, false));
        // The above rule should not be applied to any transaction, since there exists no category with id 83818238
        Helper.postCategoryRule(sessionID,
                new CategoryRule("", "L43", "", categoryIDs.get(0), false));
        Helper.postCategoryRule(sessionID,
                new CategoryRule("", "", "withdrawal", categoryIDs.get(1), false));
        Helper.postCategoryRule(sessionID,
                new CategoryRule("abc", "", "", categoryIDs.get(2), false));
        Helper.postCategoryRule(sessionID,
                new CategoryRule("", "ING", "", categoryIDs.get(0), false));

        // Create transactions
        ArrayList<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction("2018-04-13T08:06:10.000Z",
                100, "abc", "NL39RABO0300065264", "deposit"));
        transactions.add(new Transaction("2017-04-13T08:06:10.000Z",
                100, "def", "NL40RABO0300065264", "withdrawal"));
        transactions.add(new Transaction("2016-04-13T08:06:10.000Z",
                100, "ghi", "NL41RABO0300065264", "deposit"));
        transactions.add(new Transaction("2015-04-13T08:06:10.000Z",
                100, "jkl", "NL42RABO0300065264", "withdrawal"));
        transactions.add(new Transaction("2014-04-13T08:06:10.000Z",
                100, "mno", "NL43RABO0300065264", "deposit"));

        ArrayList<Long> transactionIDs = new ArrayList<>();
        for (Transaction transaction : transactions) {
            transactionIDs.add(Helper.postTransaction(sessionID, transaction));
        }

        // Test whether the categories are correctly assigned
        ArrayList<Long> fetchedTransactionIDs;

        fetchedTransactionIDs = Helper.filterOnCategory(sessionID, categories.get(0));
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(4)));

        fetchedTransactionIDs = Helper.filterOnCategory(sessionID, categories.get(1));
        assertThat(fetchedTransactionIDs.size(), equalTo(2));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(1)));
        assertThat(fetchedTransactionIDs.get(1), equalTo(transactionIDs.get(3)));

        fetchedTransactionIDs = Helper.filterOnCategory(sessionID, categories.get(2));
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(0)));
    }

    @Test
    public void testGetCategoryRule() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/categoryRules/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5")
                .get(Constants.PREFIX + "/categoryRules/1").then().statusCode(401);

        // Create new session
        String sessionID = Helper.getNewSessionID();

        // Test invalid categoryRule ID status code
        given().header("X-session-ID", sessionID).get(Constants.PREFIX + "/categoryRules/8381237").then().statusCode(404);

        // Create new categoryRules
        CategoryRule categoryRule1 =
                new CategoryRule("strawberry", "NL66ABNA0123456789", "deposit", 1, false);
        CategoryRule categoryRule2 =
                new CategoryRule("", "", "withdrawal", 1, true);
        long categoryRuleID1 = Helper.postCategoryRule(sessionID, categoryRule1);
        long categoryRuleID2 = Helper.postCategoryRule(sessionID, categoryRule2);

        // Test valid categoryRule response and status code
        given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/categoryRules/" + categoryRuleID1).
                then().statusCode(200).
                body("id", equalTo((int) categoryRuleID1)).
                body("description", equalTo("strawberry")).
                body("iBAN", equalTo("NL66ABNA0123456789")).
                body("type", equalTo("deposit")).
                body("category_id", equalTo(1)).
                body("applyOnHistory", equalTo(false));
        given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/categoryRules/" + categoryRuleID2).
                then().statusCode(200).
                body("id", equalTo((int) categoryRuleID2)).
                body("description", equalTo("")).
                body("iBAN", equalTo("")).
                body("type", equalTo("withdrawal")).
                body("category_id", equalTo(1)).
                body("applyOnHistory", equalTo(true));
    }

    @Test
    public void testPutCategoryRuleBasic() {
        //TODO
    }

    @Test
    public void testPutCategoryRuleNewTransaction() {
        //TODO
    }

    @Test
    public void testDeleteCategoryRuleBasic() {
        //TODO
    }

    @Test
    public void testDeleteCategoryRuleNewTransaction() {
        //TODO
    }

    @Test
    public void testApplyOnHistoryTrue() {
        //TODO
    }

    @Test
    public void testApplyOnHistoryFalse() {
        //TODO
    }

}
