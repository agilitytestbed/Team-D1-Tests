package nl.utwente.ing.testing.helper;

import io.restassured.http.ContentType;
import nl.utwente.ing.testing.bean.Category;
import nl.utwente.ing.testing.bean.Transaction;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.path.json.JsonPath.from;

public class Helper {

    public static String getNewSessionID() {
        String responseString = when().post(Constants.PREFIX + "/sessions").
                then().contentType(ContentType.JSON).
                extract().response().asString();
        Map<String, ?> responseMap = from(responseString).get("");
        return (String) responseMap.get("id");
    }

    public static Long postTransaction(String sessionID, Transaction transaction) {
        String responseString = given().contentType("application/json").
                body(transaction).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/transactions").
                then().contentType(ContentType.JSON).extract().response().asString();
        Map<String, ?> responseMap = from(responseString).get("");
        return new Long((Integer) responseMap.get("id"));
    }

    public static Long postCategory(String sessionID, Category category) {
        String responseString = given().contentType("application/json").
                body(category).header("X-session-ID", sessionID).
                post(Constants.PREFIX + "/categories").
                then().contentType(ContentType.JSON).extract().response().asString();
        Map<String, ?> responseMap = from(responseString).get("");
        return new Long((Integer) responseMap.get("id"));
    }

    public static void assignCategoryToTransaction(String sessionID, long transactionID, long categoryID) {
        Map<String, Long> categoryIDMap = new HashMap<>();
        categoryIDMap.put("category_id", categoryID);
        given().header("X-session-ID", sessionID).
                contentType("application/json").body(categoryIDMap).
                patch(Constants.PREFIX + "/transactions/" + transactionID + "/category");
    }

}
