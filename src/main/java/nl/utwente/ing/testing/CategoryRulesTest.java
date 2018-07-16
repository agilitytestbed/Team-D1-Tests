package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.CategoryRule;
import nl.utwente.ing.testing.bean.CategoryRuleWithoutApplyOnHistory;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.MatcherAssert.assertThat;

public class CategoryRulesTest {

    @Test
    public void testGetCategoryRules() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/categoryRules").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/categoryRules").then().statusCode(401);

        // Test responses and status codes
        String newSessionID = RequestHelper.getNewSessionID();
        ArrayList<CategoryRule> categoryRules = new ArrayList<>();
        categoryRules.add(new CategoryRule("strawberry", "NL66ABNA0123456789", "deposit", 1, false));
        categoryRules.add(new CategoryRule("blueberry", "NL67ABNA0123456789", "deposit", 2, false));
        categoryRules.add(new CategoryRule("raspberry", "NL68ABNA0123456789", "deposit", 3, false));
        categoryRules.add(new CategoryRule("", "NL69ABNA0123456789", "withdrawal", 4, true));
        for (CategoryRule categoryRule : categoryRules) {
            RequestHelper.postCategoryRule(newSessionID, categoryRule);
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
        String sessionID = RequestHelper.getNewSessionID();

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
        String sessionID = RequestHelper.getNewSessionID();

        // Create categories
        ArrayList<Category> categories = new ArrayList<>();
        categories.add(new Category("Apple"));
        categories.add(new Category("Pear"));
        categories.add(new Category("Strawberry"));

        ArrayList<Long> categoryIDs = new ArrayList<>();
        for (Category category : categories) {
            categoryIDs.add(RequestHelper.postCategory(sessionID, category));
        }

        // Create categoryRules
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "", "deposit", 83818238, false));
        // The above rule should not be applied to any transaction, since there exists no category with id 83818238
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "L43", "", categoryIDs.get(0), false));
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "", "withdrawal", categoryIDs.get(1), false));
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("abc", "", "", categoryIDs.get(2), false));
        RequestHelper.postCategoryRule(sessionID,
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
            transactionIDs.add(RequestHelper.postTransaction(sessionID, transaction));
        }

        // Test whether the categories are correctly assigned
        ArrayList<Long> fetchedTransactionIDs;

        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, categories.get(0));
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(4)));

        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, categories.get(1));
        assertThat(fetchedTransactionIDs.size(), equalTo(2));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(1)));
        assertThat(fetchedTransactionIDs.get(1), equalTo(transactionIDs.get(3)));

        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, categories.get(2));
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
        String sessionID = RequestHelper.getNewSessionID();

        // Test invalid categoryRule ID status code
        given().header("X-session-ID", sessionID).get(Constants.PREFIX + "/categoryRules/8381237").then().statusCode(404);

        // Create new categoryRules
        CategoryRule categoryRule1 =
                new CategoryRule("strawberry", "NL66ABNA0123456789", "deposit", 1, false);
        CategoryRule categoryRule2 =
                new CategoryRule("", "", "withdrawal", 1, true);
        long categoryRuleID1 = RequestHelper.postCategoryRule(sessionID, categoryRule1);
        long categoryRuleID2 = RequestHelper.postCategoryRule(sessionID, categoryRule2);

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
        // Set up test
        String sessionID = RequestHelper.getNewSessionID();
        CategoryRule categoryRule =
                new CategoryRule("description", "iban", "deposit", 1, false);
        long categoryRuleID = RequestHelper.postCategoryRule(sessionID, categoryRule);
        CategoryRuleWithoutApplyOnHistory newCategoryRule =
                new CategoryRuleWithoutApplyOnHistory("noitpircsed", "nabi", "withdrawal", 2);

        // Test invalid session ID status code
        given().contentType("application/json").
                body(newCategoryRule).
                put(Constants.PREFIX + "/categoryRules/1").then().statusCode(401);
        given().contentType("application/json").
                body(newCategoryRule).header("X-session-ID", "A1B2C3D4E5").
                put(Constants.PREFIX + "/categoryRules/1").then().statusCode(401);

        // Test invalid categoryRuleID status code
        given().contentType("application/json").body(newCategoryRule).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/8381237").then().statusCode(404);

        // Test valid categoryRule put response and status code
        given().contentType("application/json").
                body(newCategoryRule).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/" + categoryRuleID).
                then().statusCode(200).
                body("id", equalTo((int) categoryRuleID)).
                body("description", equalTo("noitpircsed")).
                body("iBAN", equalTo("nabi")).
                body("type", equalTo("withdrawal")).
                body("category_id", equalTo(2)).
                body("applyOnHistory", equalTo(false));

        // Test invalid input status code
        newCategoryRule.setType("xxx");
        given().contentType("application/json").
                body(newCategoryRule).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/" + categoryRuleID).then().statusCode(405);
    }

    @Test
    public void testPutCategoryRuleNewTransaction() {
        String sessionID = RequestHelper.getNewSessionID();

        // Create categories
        ArrayList<Category> categories = new ArrayList<>();
        categories.add(new Category("University"));
        categories.add(new Category("Rent"));
        categories.add(new Category("Food"));

        ArrayList<Long> categoryIDs = new ArrayList<>();
        for (Category category : categories) {
            categoryIDs.add(RequestHelper.postCategory(sessionID, category));
        }

        // Create categoryRules
        long categoryRuleID1 = RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "", "deposit", 83818238, false));
        // The above rule should not be applied to any transaction, since there exists no category with id 83818238
        long categoryRuleID2 = RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "L43", "", categoryIDs.get(0), false));
        long categoryRuleID3 = RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "", "withdrawal", categoryIDs.get(1), false));
        long categoryRuleID4 = RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("abc", "", "", categoryIDs.get(2), false));
        long categoryRuleID5 = RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "ING", "", categoryIDs.get(0), false));

        // Update categoryRules
        CategoryRuleWithoutApplyOnHistory categoryRule2New =
                new CategoryRuleWithoutApplyOnHistory("", "L43", "", categoryIDs.get(1));
        CategoryRuleWithoutApplyOnHistory categoryRule3New =
                new CategoryRuleWithoutApplyOnHistory("", "", "withdrawal", categoryIDs.get(2));
        CategoryRuleWithoutApplyOnHistory categoryRule4New =
                new CategoryRuleWithoutApplyOnHistory("ghi", "", "", categoryIDs.get(0));
        CategoryRuleWithoutApplyOnHistory categoryRule5New =
                new CategoryRuleWithoutApplyOnHistory("", "ING", "", categoryIDs.get(1));
        given().contentType("application/json").
                body(categoryRule2New).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/" + categoryRuleID2);
        given().contentType("application/json").
                body(categoryRule3New).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/" + categoryRuleID3);
        given().contentType("application/json").
                body(categoryRule4New).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/" + categoryRuleID4);
        given().contentType("application/json").
                body(categoryRule5New).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categoryRules/" + categoryRuleID5);

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
            transactionIDs.add(RequestHelper.postTransaction(sessionID, transaction));
        }

        // Test whether the categories are correctly assigned (by the updated categoryRules)
        ArrayList<Long> fetchedTransactionIDs;

        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, categories.get(0));
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(2)));

        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, categories.get(1));
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(4)));

        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, categories.get(2));
        assertThat(fetchedTransactionIDs.size(), equalTo(2));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionIDs.get(1)));
        assertThat(fetchedTransactionIDs.get(1), equalTo(transactionIDs.get(3)));
    }

    @Test
    public void testDeleteCategoryRuleBasic() {
        // Test invalid session ID status code
        when().delete(Constants.PREFIX + "/categoryRules/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").delete(Constants.PREFIX + "/categoryRules/1").then().statusCode(401);

        // Test invalid categoryRuleID status code
        String sessionID = RequestHelper.getNewSessionID();
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/categoryRules/8381237").then().statusCode(404);

        // Test valid categoryRule status code
        CategoryRule categoryRule =
                new CategoryRule("strawberry", "NL66ABNA0123456789", "deposit", 1, false);
        long categoryRuleID = RequestHelper.postCategoryRule(sessionID, categoryRule);
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/categoryRules/" + categoryRuleID).
                then().statusCode(204);
    }

    @Test
    public void testDeleteCategoryRuleNewTransaction() {
        // Set up the test
        String sessionID = RequestHelper.getNewSessionID();
        Category category = new Category("Sugar");
        long categoryID = RequestHelper.postCategory(sessionID, category);
        long categoryRuleID = RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("cake", "", "", categoryID, false));

        /* First post a transaction to which the categoryRule applies, then delete the categoryRule, then post another
        transaction to which the categoryRule would apply, finally test that the category is only assigned to the first
        transaction. */
        long transactionID1 = RequestHelper.postTransaction(sessionID, new Transaction("2014-04-13T08:06:10.000Z",
                20, "Birthday cake", "NL43RABO0300065264", "deposit"));
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/categoryRules/" + categoryRuleID);
        long transactionID2 = RequestHelper.postTransaction(sessionID, new Transaction("2014-04-13T08:06:17.000Z",
                5, "Recipe to bake cake made of apples", "NL43RABO0300065264", "deposit"));

        ArrayList<Long> fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, category);
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionID1));
    }

    @Test
    public void testApplyOnHistoryTrue() {
        // Set up the test
        String sessionID = RequestHelper.getNewSessionID();
        Category category1 = new Category("Sugar");
        long categoryID1 = RequestHelper.postCategory(sessionID, category1);
        Category category2 = new Category("Vegetables");
        long categoryID2 = RequestHelper.postCategory(sessionID, category2);

        /* First post transactions to which the categoryRule would apply, then post the categoryRule, finally test if
        the category is applied to the transactions. */
        long transactionID1 = RequestHelper.postTransaction(sessionID, new Transaction("2014-04-13T08:06:10.000Z",
                20, "Birthday cake", "NL43RABO0300065264", "deposit"));
        long transactionID2 = RequestHelper.postTransaction(sessionID, new Transaction("2014-04-14T08:06:10.000Z",
                1, "Healthy cake made of broccoli", "NL43RABO0300065264", "deposit"));
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("cake", "", "", categoryID1, true));

        ArrayList<Long> fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, category1);
        assertThat(fetchedTransactionIDs.size(), equalTo(2));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionID1));
        assertThat(fetchedTransactionIDs.get(1), equalTo(transactionID2));

        // Test that a categoryRule with an invalid categoryID is not applied on history
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("", "", "deposit", 71371281, true));
        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, category1);
        assertThat(fetchedTransactionIDs.size(), equalTo(2));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionID1));
        assertThat(fetchedTransactionIDs.get(1), equalTo(transactionID2));

        // Test whether a new categoryRule applied on history also applies to categories that already had a category
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("broccoli", "", "", categoryID2, true));
        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, category1);
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionID1));
        fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, category2);
        assertThat(fetchedTransactionIDs.size(), equalTo(1));
        assertThat(fetchedTransactionIDs.get(0), equalTo(transactionID2));
    }

    @Test
    public void testApplyOnHistoryFalse() {
        // Set up the test
        String sessionID = RequestHelper.getNewSessionID();
        Category category = new Category("Sugar");
        long categoryID = RequestHelper.postCategory(sessionID, category);

        /* First post a transaction, then post a categoryRule that would have applied to it, finally check that no
         * category is assigned to the transaction. */
        long transactionID = RequestHelper.postTransaction(sessionID, new Transaction("2014-04-13T08:06:10.000Z",
                20, "Birthday cake", "NL43RABO0300065264", "deposit"));
        RequestHelper.postCategoryRule(sessionID,
                new CategoryRule("cake", "", "", categoryID, false));

        ArrayList<Long> fetchedTransactionIDs = RequestHelper.filterOnCategory(sessionID, category);
        assertThat(fetchedTransactionIDs.size(), equalTo(0));
    }

}
