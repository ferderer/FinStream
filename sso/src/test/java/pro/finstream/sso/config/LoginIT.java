package pro.finstream.sso.config;

import static io.restassured.RestAssured.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static pro.finstream.sso.support.test.TestUsers.VADIM_TEST;
import pro.finstream.sso.support.test.WebIntegrationTestBase;

class LoginIT extends WebIntegrationTestBase {
    
    @Test
    void canDoFormLogin() throws Exception {
        var sid = given()
            .accept(ContentType.HTML)
        .when()
            .get("/login")
            .cookie("sid");

        given()
            .sessionId(sid)
            .cookie("sid", sid)
            .csrf("/login")
            .contentType(ContentType.URLENC)
            .formParam("username", VADIM_TEST.username())
            .formParam("password", VADIM_TEST.password())
        .when()
            .post("/login")
        .then()
            .statusCode(302);
    }
}
