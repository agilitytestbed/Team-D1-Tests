package nl.utwente.ing.testing;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.Helper;
import nl.utwente.ing.testing.helper.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static io.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class InitialSystemTest {

    public static String sessionID;

    @BeforeAll
    public static void setup() {
        sessionID = Helper.getNewSessionID();
    }

    @Test
    public void testSessionIDRetrieval() {
        // Test status code and that body contains id field
        when().post(Constants.PREFIX + "/sessions").
                then().statusCode(201).
                body("$", hasKey("id"));
    }

    @Test
    public void testGetTransactions() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/transactions").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/transactions").then().statusCode(401);

        // Test responses and status codes
        String newSessionID = Helper.getNewSessionID();
        ArrayList<Transaction> transactionList = new ArrayList<>();
        transactionList.add(new Transaction("2015-04-13T08:06:10.000Z",
                100, "NL01RABO0300065264", "deposit"));
        transactionList.add(new Transaction("2016-04-13T08:06:10.000Z",
                200, "NL02RABO0300065264", "withdrawal"));
        transactionList.add(new Transaction("2017-04-13T08:06:10.000Z",
                300, "NL03RABO0300065264", "deposit"));
        transactionList.add(new Transaction("2018-04-13T08:06:10.000Z",
                400, "NL04RABO0300065264", "withdrawal"));
        for (Transaction transaction : transactionList) {
            Helper.postTransaction(newSessionID, transaction);
        }

        for (int i = 0; i < transactionList.size(); i++) {
            String responseString = given().header("X-session-ID", newSessionID).
                    queryParam("limit", 1).queryParam("offset", i).
                    get(Constants.PREFIX + "/transactions").
                    then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
            ArrayList<Map<String, ?>> responseList = from(responseString).get("");
            assertThat(responseList.size(), equalTo(1));
            assertThat((String) responseList.get(0).get("date"), equalTo(transactionList.get(i).getDate()));
            assertThat((Float) responseList.get(0).get("amount"), equalTo(transactionList.get(i).getAmount()));
            assertThat((String) responseList.get(0).get("externalIBAN"), equalTo(transactionList.get(i).getExternalIBAN()));
            assertThat((String) responseList.get(0).get("type"), equalTo(transactionList.get(i).getType()));
        }
        String responseString = given().header("X-session-ID", newSessionID).
                get(Constants.PREFIX + "/transactions").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(4));
        for (int i = 0; i < transactionList.size(); i++) {
            assertThat((String) responseList.get(i).get("date"), equalTo(transactionList.get(i).getDate()));
            assertThat((Float) responseList.get(i).get("amount"), equalTo(transactionList.get(i).getAmount()));
            assertThat((String) responseList.get(i).get("externalIBAN"), equalTo(transactionList.get(i).getExternalIBAN()));
            assertThat((String) responseList.get(i).get("type"), equalTo(transactionList.get(i).getType()));
        }
    }

    @Test
    public void testPostTransaction() {
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");

        // Test invalid session ID status code
        given().contentType("application/json").
                body(transaction).
                post(Constants.PREFIX + "/transactions").then().statusCode(401);
        given().contentType("application/json").
                body(transaction).header("X-session-ID", "A1B2C3D4E5").
                post(Constants.PREFIX + "/transactions").then().statusCode(401);

        // Test valid transaction post response and status code
        given().contentType("application/json").
                body(transaction).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/transactions").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("date", equalTo("2018-04-13T08:06:10.000Z")).
                body("amount", equalTo((float) 100)).
                body("externalIBAN", equalTo("NL39RABO0300065264")).
                body("type", equalTo("deposit"));

        // Test invalid input status code
        transaction.setType("xxx");
        given().contentType("application/json").
                body(transaction).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/transactions").then().statusCode(405);
    }

    @Test
    public void testGetTransaction() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/transactions/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/transactions/1").then().statusCode(401);

        // Test invalid transaction ID status code
        given().header("X-session-ID", sessionID).get(Constants.PREFIX + "/transactions/8381237").then().statusCode(404);

        // Test valid transaction response and status code
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");
        long transactionID = Helper.postTransaction(sessionID, transaction);
        given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/transactions/" + transactionID).
                then().statusCode(200).
                body("$", hasKey("id")).
                body("date", equalTo("2018-04-13T08:06:10.000Z")).
                body("amount", equalTo((float) 100)).
                body("externalIBAN", equalTo("NL39RABO0300065264")).
                body("type", equalTo("deposit"));
    }

    @Test
    public void testPutTransaction() {
        Transaction transaction = new Transaction("2015-04-13T08:06:10.000Z",
                75, "NL01RABO0300065264", "withdrawal");

        // Test invalid session ID status code
        given().contentType("application/json").
                body(transaction).
                put(Constants.PREFIX + "/transactions/1").then().statusCode(401);
        given().contentType("application/json").
                body(transaction).header("X-session-ID", "A1B2C3D4E5").
                put(Constants.PREFIX + "/transactions/1").then().statusCode(401);

        // Test invalid transaction ID status code
        given().contentType("application/json").body(transaction).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/transactions/8381237").then().statusCode(404);

        // Test valid transaction put response and status code
        long transactionID = Helper.postTransaction(sessionID, transaction);
        transaction.setDate("2013-04-13T08:06:10.000Z");
        transaction.setAmount(225);
        transaction.setExternalIBAN("NL02RABO0300065264");
        transaction.setType("deposit");
        given().contentType("application/json").
                body(transaction).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/transactions/" + transactionID).
                then().statusCode(200).
                body("$", hasKey("id")).
                body("date", equalTo("2013-04-13T08:06:10.000Z")).
                body("amount", equalTo((float) 225)).
                body("externalIBAN", equalTo("NL02RABO0300065264")).
                body("type", equalTo("deposit"));

        // Test invalid input status code
        transaction.setType("xxx");
        given().contentType("application/json").
                body(transaction).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/transactions/" + transactionID).then().statusCode(405);
    }

    @Test
    public void testDeleteTransaction() {
        // Test invalid session ID status code
        when().delete(Constants.PREFIX + "/transactions/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").delete(Constants.PREFIX + "/transactions/1").then().statusCode(401);

        // Test invalid transaction ID status code
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/transactions/8381237").then().statusCode(404);

        // Test valid transaction status code
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");
        long transactionID = Helper.postTransaction(sessionID, transaction);
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/transactions/" + transactionID).
                then().statusCode(204);
    }

    @Test
    public void testAssignCategoryToTransaction() {
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");
        Category category1 = new Category("Groceries");
        Category category2 = new Category("Rent");
        long transactionID = Helper.postTransaction(sessionID, transaction);
        long categoryID1 = Helper.postCategory(sessionID, category1);
        long categoryID2 = Helper.postCategory(sessionID, category2);
        Map<String, Long> categoryIDMap = new HashMap<>();
        categoryIDMap.put("category_id", categoryID1);

        // Test invalid session ID status code
        given().contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/1/category").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").
                contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/1/category").then().statusCode(401);

        // Test valid assignment
        String responseString = given().header("X-session-ID", sessionID).
                contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/" + transactionID + "/category").
                then().statusCode(200).
                contentType(ContentType.JSON).extract().response().asString();
        Map<String, ?> responseMap = from(responseString).get("category");
        assertThat((String) responseMap.get("name"), equalTo(category1.getName()));
        assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID1));
        categoryIDMap.put("category_id", categoryID2);
        responseString = given().header("X-session-ID", sessionID).
                contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/" + transactionID + "/category").
                then().statusCode(200).
                contentType(ContentType.JSON).extract().response().asString();
        responseMap = from(responseString).get("category");
        assertThat((String) responseMap.get("name"), equalTo(category2.getName()));
        assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID2));

        // Test invalid transaction ID and invalid category ID
        given().header("X-session-ID", sessionID).
                contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/7183291/category").
                then().statusCode(404);
        categoryIDMap.put("category_id", new Long(7183291));
        given().header("X-session-ID", sessionID).
                contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/" + transactionID + "/category").
                then().statusCode(404);
    }

    @Test
    public void testGetCategories() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/categories").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/categories").then().statusCode(401);

        // Test responses and status codes
        String newSessionID = Helper.getNewSessionID();
        ArrayList<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category("Groceries"));
        categoryList.add(new Category("Rent"));
        categoryList.add(new Category("Entertainment"));
        categoryList.add(new Category("Salary"));
        for (Category category : categoryList) {
            Helper.postCategory(newSessionID, category);
        }

        for (int i = 0; i < categoryList.size(); i++) {
            String responseString = given().header("X-session-ID", newSessionID).
                    queryParam("limit", 1).queryParam("offset", i).
                    get(Constants.PREFIX + "/categories").
                    then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
            ArrayList<Map<String, ?>> responseList = from(responseString).get("");
            assertThat(responseList.size(), equalTo(1));
            assertThat((String) responseList.get(0).get("name"), equalTo(categoryList.get(i).getName()));
        }
        String responseString = given().header("X-session-ID", newSessionID).
                get(Constants.PREFIX + "/categories").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(4));
        for (int i = 0; i < categoryList.size(); i++) {
            assertThat((String) responseList.get(i).get("name"), equalTo(categoryList.get(i).getName()));
        }
    }

    @Test
    public void testPostCategory() {
        Category category = new Category("Groceries");

        // Test invalid session ID status code
        given().contentType("application/json").
                body(category).
                post(Constants.PREFIX + "/categories").then().statusCode(401);
        given().contentType("application/json").
                body(category).header("X-session-ID", "A1B2C3D4E5").
                post(Constants.PREFIX + "/categories").then().statusCode(401);

        // Test valid category post response and status code
        given().contentType("application/json").
                body(category).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categories").
                then().statusCode(201).
                body("$", hasKey("id")).
                body("name", equalTo("Groceries"));

        // Test invalid input status code
        category.setName(null);
        given().contentType("application/json").
                body(category).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categories").then().statusCode(405);
    }

    @Test
    public void testGetCategory() {
        // Test invalid session ID status code
        when().get(Constants.PREFIX + "/categories/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").get(Constants.PREFIX + "/categories/1").then().statusCode(401);

        // Test invalid category ID status code
        given().header("X-session-ID", sessionID).get(Constants.PREFIX + "/categories/8381237").then().statusCode(404);

        // Test valid category response and status code
        Category category = new Category("Groceries");
        long categoryID = Helper.postCategory(sessionID, category);
        given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/categories/" + categoryID).
                then().statusCode(200).
                body("name", equalTo("Groceries"));
    }

    @Test
    public void testPutCategory() {
        Category category = new Category("Groceries");

        // Test invalid session ID status code
        given().contentType("application/json").
                body(category).
                put(Constants.PREFIX + "/categories/1").then().statusCode(401);
        given().contentType("application/json").
                body(category).header("X-session-ID", "A1B2C3D4E5").
                put(Constants.PREFIX + "/categories/1").then().statusCode(401);

        // Test invalid catery ID status code
        given().contentType("application/json").body(category).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categories/8381237").then().statusCode(404);

        // Test valid category put response and status code
        long categoryID = Helper.postCategory(sessionID, category);
        category.setName("Rent");
        given().contentType("application/json").
                body(category).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categories/" + categoryID).
                then().statusCode(200).
                body("$", hasKey("id")).
                body("name", equalTo("Rent"));

        // Test invalid input status code
        category.setName(null);
        given().contentType("application/json").
                body(category).header("X-session-ID", sessionID).
                put(Constants.PREFIX + "/categories/" + categoryID).then().statusCode(405);
    }

    @Test
    public void deleteCategory() {
        // Test invalid session ID status code
        when().delete(Constants.PREFIX + "/categories/1").then().statusCode(401);
        given().header("X-session-ID", "A1B2C3D4E5").delete(Constants.PREFIX + "/categories/1").then().statusCode(401);

        // Test invalid category ID status code
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/categories/8381237").then().statusCode(404);

        // Test valid category status code
        Category category = new Category("Groceries");
        long categoryID = Helper.postCategory(sessionID, category);
        given().header("X-session-ID", sessionID).delete(Constants.PREFIX + "/categories/" + categoryID).
                then().statusCode(204);
    }

    @Test
    public void categoryAtGetTransactions() {
        // Used to test whether the category is correctly displayed in the getTransactions request
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");
        Category category = new Category("Groceries");
        String newSessionID = Helper.getNewSessionID();
        long transactionID = Helper.postTransaction(newSessionID, transaction);
        long categoryID = Helper.postCategory(newSessionID, category);
        Helper.assignCategoryToTransaction(newSessionID, transactionID, categoryID);
        String responseString = given().header("X-session-ID", newSessionID).get(Constants.PREFIX + "/transactions").
                then().statusCode(200).
                contentType(ContentType.JSON).extract().response().asString();
        Map<String, ?> responseMap = (Map<String, ?>) ((ArrayList<Map<String, ?>>) from(responseString).get("")).get(0);
        assertThat((String) responseMap.get("date"), equalTo(transaction.getDate()));
        assertThat((Float) responseMap.get("amount"), equalTo(transaction.getAmount()));
        assertThat((String) responseMap.get("externalIBAN"), equalTo(transaction.getExternalIBAN()));
        assertThat((String) responseMap.get("type"), equalTo(transaction.getType()));
        responseMap = (Map<String, ?>) responseMap.get("category");
        assertThat((String) responseMap.get("name"), equalTo(category.getName()));
        assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID));
    }

    @Test
    public void testCategoryAtGetTransaction() {
        // Used to test whether the category is correctly displayed in the getTransaction request
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");
        Category category = new Category("Groceries");
        long transactionID = Helper.postTransaction(sessionID, transaction);
        long categoryID = Helper.postCategory(sessionID, category);
        Helper.assignCategoryToTransaction(sessionID, transactionID, categoryID);
        String responseString = given().header("X-session-ID", sessionID).
                get(Constants.PREFIX + "/transactions/" + transactionID).
                then().statusCode(200).
                body("$", hasKey("id")).
                body("date", equalTo("2018-04-13T08:06:10.000Z")).
                body("amount", equalTo((float) 100)).
                body("externalIBAN", equalTo("NL39RABO0300065264")).
                body("type", equalTo("deposit")).
                contentType(ContentType.JSON).extract().response().asString();
        Map<String, ?> responseMap = from(responseString).get("category");
        assertThat((String) responseMap.get("name"), equalTo(category.getName()));
        assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID));
    }

    @Test
    public void testCategoryAtPutTransaction() {
        // Used to test whether the category is correctly displayed in the putTransaction request
        Transaction transaction = new Transaction("2018-04-13T08:06:10.000Z",
                100, "NL39RABO0300065264", "deposit");
        Category category = new Category("Groceries");
        long transactionID = Helper.postTransaction(sessionID, transaction);
        long categoryID = Helper.postCategory(sessionID, category);
        Helper.assignCategoryToTransaction(sessionID, transactionID, categoryID);
        String responseString = given().header("X-session-ID", sessionID).
                contentType("application/json").body(transaction).
                put(Constants.PREFIX + "/transactions/" + transactionID).
                then().statusCode(200).
                body("$", hasKey("id")).
                body("date", equalTo("2018-04-13T08:06:10.000Z")).
                body("amount", equalTo((float) 100)).
                body("externalIBAN", equalTo("NL39RABO0300065264")).
                body("type", equalTo("deposit")).
                contentType(ContentType.JSON).extract().response().asString();
        Map<String, ?> responseMap = from(responseString).get("category");
        assertThat((String) responseMap.get("name"), equalTo(category.getName()));
        assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID));
    }

    @Test
    public void testFilterOnCategories() {
        // Used to test whether filtering on categories works correctly in the getTransactions request
        Category category1 = new Category("Groceries");
        Category category2 = new Category("Rent");
        ArrayList<Transaction> transactionList = new ArrayList<>();
        transactionList.add(new Transaction("2015-04-13T08:06:10.000Z",
                100, "NL01RABO0300065264", "deposit"));
        transactionList.add(new Transaction("2016-04-13T08:06:10.000Z",
                200, "NL02RABO0300065264", "withdrawal"));
        transactionList.add(new Transaction("2017-04-13T08:06:10.000Z",
                300, "NL03RABO0300065264", "deposit"));
        transactionList.add(new Transaction("2018-04-13T08:06:10.000Z",
                400, "NL04RABO0300065264", "withdrawal"));
        String newSessionID = Helper.getNewSessionID();
        long categoryID1 = Helper.postCategory(newSessionID, category1);
        long categoryID2 = Helper.postCategory(newSessionID, category2);
        for (Transaction transaction : transactionList) {
            long transactionID = Helper.postTransaction(newSessionID, transaction);
            if (transactionID % 2 == 1) {
                Helper.assignCategoryToTransaction(newSessionID, transactionID, categoryID1);
            } else {
                Helper.assignCategoryToTransaction(newSessionID, transactionID, categoryID2);
            }
        }

        String responseString = given().header("X-session-ID", newSessionID).
                queryParam("category", "Groceries").
                get(Constants.PREFIX + "/transactions").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        ArrayList<Map<String, ?>> responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(2));
        for (int i = 0; i < 2; i++) {
            long transactionID = new Long((Integer) responseList.get(i).get("id"));
            Map<String, ?> responseMap = (Map<String, ?>) responseList.get(i).get("category");
            assertThat((String) responseMap.get("name"), equalTo(category1.getName()));
            assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID1));
        }
        responseString = given().header("X-session-ID", newSessionID).
                queryParam("category", "Rent").
                get(Constants.PREFIX + "/transactions").
                then().statusCode(200).contentType(ContentType.JSON).extract().response().asString();
        responseList = from(responseString).get("");
        assertThat(responseList.size(), equalTo(2));
        for (int i = 0; i < 2; i++) {
            long transactionID = new Long((Integer) responseList.get(i).get("id"));
            Map<String, ?> responseMap = (Map<String, ?>) responseList.get(i).get("category");
            assertThat((String) responseMap.get("name"), equalTo(category2.getName()));
            assertThat(new Long((Integer) responseMap.get("id")), equalTo(categoryID2));
        }
    }

}