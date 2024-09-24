package org.example.calendar.auth;

import org.springframework.test.context.jdbc.Sql;
import org.example.calendar.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.http.Cookie;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.util.Map;

/*
    In all the tests below that we log in a user, since we are using the DaoAuthenticationProvider upon successful
    authentication, the CsrfAuthenticationStrategy will be invoked, to invalid the previous csrf token and assign a new
    one. This is the reason that we update the cookiesMap, as cookiesMap = CookieTestUtils.parseCookies(headers); after
    the log in response, to have access to the new valid csrf.

    @SQL https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/executing-sql.html

    After removing the credentials for sending emails, and replaced them with placeholders those tests will fail because
    it will try to find the values, and they will not be present. The error message does not give any information
        ${MAIL_USERNAME}, ${MAIL_PASSWORD}
    The EventIT test pass because the Test Profile is Active and replaces those values
 */
@Sql("/scripts/INIT_USERS.sql")
class AuthIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";

    // register()
    @Test
    void shouldRegisterUser() {
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .statusCode(200)
                .extract()
                .response();

        Map<String, String> cookies = response.getCookies();

        String username = FAKER.internet().username();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(12, 128, true, true, true);

        String requestBody = String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);

        given()
                .contentType(ContentType.JSON)
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .body(requestBody)
                .when()
                .post(AUTH_PATH + "/register")
                .then()
                .cookie("ACCESS_TOKEN")
                .cookie("REFRESH_TOKEN")
                .statusCode(201);
    }

    // login()
    @Test
    void shouldLogin() {
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .statusCode(200)
                .extract()
                .response();

        Map<String, String> cookies = response.getCookies();

        given()
                .contentType(ContentType.URLENC)
                .formParam("username", userCredentials().get(0).getFirst())
                .formParam("password", userCredentials().get(0).getSecond())
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post("/login")
                .then()
                .cookie("ACCESS_TOKEN")
                .cookie("REFRESH_TOKEN")
                .extract()
                .response();
    }

    // refresh()
    @Test
    void shouldRefreshToken() {
        // Get csrf token from response header
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .statusCode(200)
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

        /*
            The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
            We need to hold the value the csrf token has for our next assertions because in the next request when we
            acquire the new Refresh/Access token, we lose the value of the csrf
         */
        cookies = response.getCookies();
        String csrfTokenValue = cookies.get("XSRF-TOKEN");
        String oldAccessTokenValue = cookies.get("ACCESS_TOKEN");
        String oldRefreshTokenValue = cookies.get("REFRESH_TOKEN");

        await().pollDelay(Duration.ofSeconds(1)).until(() -> true);

        /*
            We perform 3 assertions
                Case 1: Access and refresh token are set on the response headers
                Case 2: The value of the new refresh token does not match the value of the refresh token that was submitted
                for the request. The old access token and the new one also do not match
                Case 3: Performing a POST request to "/refresh" with the old refresh token leads to 401
         */
        response = given()
                .cookie("REFRESH_TOKEN", oldRefreshTokenValue)
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post(AUTH_PATH + "/token/refresh")
                .then()
                .cookie("ACCESS_TOKEN")
                .cookie("REFRESH_TOKEN")
                .extract()
                .response();

        // At this point we lose the csrf value, since the map from the /refresh endpoint contains only Refresh/Access token
        cookies = response.getCookies();
        String newAccessTokenValue = cookies.get("ACCESS_TOKEN");
        String newRefreshTokenValue = cookies.get("REFRESH_TOKEN");

        assertThat(newAccessTokenValue).isNotEqualTo(oldAccessTokenValue);
        assertThat(newRefreshTokenValue).isNotEqualTo(oldRefreshTokenValue);

        given()
                .cookie("REFRESH_TOKEN", oldRefreshTokenValue)
                .cookie("XSRF-TOKEN", csrfTokenValue)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .when()
                .post(AUTH_PATH + "/token/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldRevokeAccessAndRefreshToken() {
        // Get csrf token from response header
        Response response = given()
                .when()
                .get(AUTH_PATH + "/token/csrf")
                .then()
                .statusCode(200)
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

        // Revoke the access token by asserting that the response will contain cookies with empty value "" and maxAge 0
        response = given()
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .post(AUTH_PATH + "/token/revoke")
                .then()
                .assertThat()
                .cookie("ACCESS_TOKEN", "")
                .cookie("REFRESH_TOKEN", "")
                .extract()
                .response();

        // io.restassured.http.Cookie; not jakarta.servlet.http.Cookie
        Cookie accessToken = response.getDetailedCookie("ACCESS_TOKEN");
        Cookie refreshToken = response.getDetailedCookie("REFRESH_TOKEN");

        assertThat(accessToken.getMaxAge()).isZero();
        assertThat(refreshToken.getMaxAge()).isZero();
    }
}