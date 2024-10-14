package org.example.calendar.user;

import org.example.calendar.user.dto.UserProfile;
import org.example.calendar.AbstractIntegrationTest;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.common.mapper.TypeRef;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
    @Sql(scripts = "/scripts/INIT_USERS.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
    Using the above at class level and the following method in the abstract class will not work.

    @AfterEach
    void clear() {
        this.userRepository.deleteAll();
    }

    Our script of adding users is executed once for the entire class, but we delete them after each test, which causes
    the remaining tests to fail, since there aren't any users.

    I decided to keep the @AfterEach and pass the script in every test. Apart from providing a clean state it also
    deletes all the other records we might have that have the user_id ON DELETE CASCADE

    @Sql(scripts = "/scripts/INIT_USERS.sql")
    class UserIT extends AbstractIntegrationTest -> Won't work either without executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
    because every method level @Sql script will overwrite it.

    After removing the credentials for sending emails, and replaced them with placeholders those tests will fail because
    it will try to find the values, and they will not be present. The error message does not give any information
        ${MAIL_USERNAME}, ${MAIL_PASSWORD}

    The EventIT test pass because the Test Profile is Active and replaces those values
 */
class UserIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String USER_PATH = "/api/v1/user";

    @Test
    @Sql(scripts = "/scripts/INIT_USERS.sql")
    void shouldSendContactRequest() {
        // Get csrf token from response header
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .extract()
                .response();

        Map<String, String> cookies = response.getCookies();

        // Login with user credentials in Spring's endpoint. The user exists in the db from the @SQL script
        response = given()
                .contentType(ContentType.URLENC)
                .formParam("username", userCredentials().get(0).getFirst())
                .formParam("password", userCredentials().get(0).getSecond())
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post("/login")
                .then()
                .extract()
                .response();
        // The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
        cookies = response.getCookies();

        // The id of the user to be added is known from the @SQL script. We could also perform a GET request to find a user
        // by something unique(email, name) and extract the id from the response.
        String requestBody = """
                {
                    "receiverId": 2
                }
                """;

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(USER_PATH + "/contacts")
                .then()
                .statusCode(200);

        response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .statusCode(200)
                .extract()
                .response();

        cookies = response.getCookies();

        // Login with the receiver and make a GET request for their pending contact requests
        response = given()
                .contentType(ContentType.URLENC)
                .formParam("username", userCredentials().get(1).getFirst())
                .formParam("password", userCredentials().get(1).getSecond())
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post("/login")
                .then()
                .extract()
                .response();

        cookies = response.getCookies();

        /*
            We could also assert on the list after parsing the response.

            List<PendingContactRequest> requests = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(USER_PATH + "/contact-requests")
                .then()
                .extract()
                .response().as(new TypeRef<>() {});
         */
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(USER_PATH + "/contact-requests")
                .then()
                .statusCode(200)
                // HamcrestMatches hasSize(), equalTo. "" refers to the root element in our case a List<PendingContactRequest>
                .body("", hasSize(1))
                // We can also assert the id, because we know it from the sql file.
                .body("[0].userProfile.name", equalTo("kris.hudson"))
                .body("[0].status", equalTo("PENDING"));
    }

    @Test
    @Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldUpdatePendingContactRequest() {
        // Get csrf token from response header
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .extract()
                .response();

        Map<String, String> cookies = response.getCookies();

        // Login with user credentials in Spring's endpoint. The user exists in the db from the @SQL script
        response = given()
                .contentType(ContentType.URLENC)
                .formParam("username", userCredentials().get(2).getFirst())
                .formParam("password", userCredentials().get(2).getSecond())
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post("/login")
                .then()
                .extract()
                .response();
        // The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
        cookies = response.getCookies();

        // The id of the user to be added is known from the @SQL script. We could also perform a GET request to find a user
        // by something unique(email, name) and extract the id from the response.
        String requestBody = """
                {
                    "senderId": 1,
                    "action": "ACCEPT"
                }
                """;

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(USER_PATH + "/contact-requests")
                .then()
                .statusCode(204);

        // After accepting/rejecting the request, we assert that the pending requests is now only 1. From the initial
        // sql script user with id 3 had 2 pending contact requests
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(USER_PATH + "/contact-requests")
                .then()
                .statusCode(200)
                // HamcrestMatches hasSize(), equalTo. "" refers to the root element in our case a List<PendingContactRequest>
                .body("", hasSize(1))
                // We can also assert the id, because we know it from the sql file.
                .body("[0].userProfile.name", equalTo("clement.gulgowski"))
                .body("[0].status", equalTo("PENDING"));
    }

    @Test
    @Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldFindContacts() {
        // Get csrf token from response header
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/")
                .then()
                .extract()
                .response();
        Map<String, String> cookies = response.getCookies();

        // Login with user credentials in Spring's endpoint. The user exists in the db from the @SQL script
        response = given()
                .contentType(ContentType.URLENC)
                .formParam("username", userCredentials().get(2).getFirst())
                .formParam("password", userCredentials().get(2).getSecond())
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post("/login")
                .then()
                .extract()
                .response();
        cookies = response.getCookies();

        /*
            The current logged in user(id = 3) has 1 contact in contacts table (2, 3) based on the sql script
            We could also assert in the response body because we know the exact values since our response is sorted by name
         */
        List<UserProfile> profiles = given()
                .accept(ContentType.JSON)
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .when()
                .get(USER_PATH + "/contacts")
                .then()
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            Each anyMatch() will check if there is a profile in profiles that match the values, if at least one is find
            returns true else returns false and our assertion fails. Our list is sorted by name.
            For more than 1 contact:
                assertThat(profiles).hasSize(1)
                    .anyMatch(userProfile -> userProfile.id().equals(2L) && userProfile.name().equals("clement.gulgowski"))
                    .anyMatch(userProfile -> userProfile.id().equals(1L) && userProfile.name().equals("kris.hudson"))
                    .isSortedAccordingTo(Comparator.comparing(UserProfile::name));
         */
        assertThat(profiles).hasSize(1)
                .anyMatch(userProfile -> userProfile.id().equals(2L) && userProfile.name().equals("clement.gulgowski"));
    }
}