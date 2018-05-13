package nl.utwente.ing.testing.schemas;

import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.Transaction;
import nl.utwente.ing.testing.helper.RequestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;


public class SchemaTest {

    public static String sessionID;
    public static Long transaction_1_ID;
    public static Long transaction_2_ID;
    public static Long category_1_ID;
    public static Long getCategory_2_ID;


    @BeforeClass
    public static void getSessionID() {
        Category category_1 = new Category(" Groceries");
        Category category_2 = new Category("Clothes");
        Transaction transaction_1 = new Transaction("2015-04-13T08:06:10.000Z",
                100, "NL01RABO0300065264", "deposit");
        Transaction transaction_2 = new Transaction("2016-04-13T08:06:10.000Z",
                200, "NL02RABO0300065264", "withdrawal");

        sessionID = RequestHelper.getNewSessionID();
        transaction_1_ID = RequestHelper.postTransaction(sessionID, transaction_1);
        transaction_2_ID = RequestHelper.postTransaction(sessionID, transaction_2);
        category_1_ID = RequestHelper.postCategory(sessionID, category_1);
        getCategory_2_ID = RequestHelper.postCategory(sessionID, category_2);
        RequestHelper.assignCategoryToTransaction(sessionID,transaction_1_ID,category_1_ID);
        RequestHelper.assignCategoryToTransaction(sessionID, transaction_2_ID, getCategory_2_ID);

    }

    @Test
    public void test_GET_transactions_JSON_schema() {

        when().
                get("/api/v1/transactions?session_id=" + sessionID).
                then().
                assertThat().body(matchesJsonSchemaInClasspath("nl/utwente/ing/testing/schemas/TransactionList.json"));

    }

    @Test
    public void test_GET_transaction_ID_JSON_schema() {

        when().
                get("/api/v1/transactions/" + transaction_1_ID.toString() + "?session_id=" + sessionID).
                then().
                assertThat().body(matchesJsonSchemaInClasspath("nl/utwente/ing/testing/schemas/Transaction.json"));

    }

    @Test
    public void test_GET_categories_JSON_schema() {

        when().
                get("/api/v1/categories?session_id=" + sessionID).
                then().
                assertThat().body(matchesJsonSchemaInClasspath("nl/utwente/ing/testing/schemas/CategoryList.json"));

    }

    @Test
    public void test_GET_category_ID_JSON_schema() {

        when().
                get("/api/v1/categories/" + category_1_ID.toString() + "?session_id=" + sessionID).
                then().
                assertThat().body(matchesJsonSchemaInClasspath("nl/utwente/ing/testing/schemas/Category.json"));

    }

}
