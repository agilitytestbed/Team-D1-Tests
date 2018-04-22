package nl.utwente.ing.testing;

import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.CategoryRule;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Constants;
import nl.utwente.ing.testing.helper.Helper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.MatcherAssert.assertThat;

public class CategoryRulesTest {

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

}
