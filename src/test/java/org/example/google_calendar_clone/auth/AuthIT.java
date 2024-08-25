package org.example.google_calendar_clone.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.example.google_calendar_clone.AbstractIntegrationTest;
import org.example.google_calendar_clone.util.TestCookieUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/*
    In all the tests below that we log in a user, since we are using the DaoAuthenticationProvider upon successful
    authentication, the CsrfAuthenticationStrategy will be invoked, to invalid the previous csrf token and assign a new
    one. This is the reason that we update the cookiesMap, as cookiesMap = CookieTestUtils.parseCookies(headers); after
    the log in response, to have access to the new valid csrf.

    @SQL https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/executing-sql.html

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(); is used to represent form data in a key-value
    format where each key can have multiple values. For example our favorite songs we can have:
        formData.add("songs", "song1");
        formData.add("songs", "song2");
        formData.add("songs", "song3");

    It can resemble forms where a form field can have multiple entries, such as checkboxes or multi-select dropdowns
 */
@AutoConfigureWebTestClient
@Sql("/scripts/INIT_USERS.sql")
class AuthIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";

    // register()
    @Test
    void shouldRegisterUser() {
        // Get csrf token from response header
        HttpHeaders headers = this.webTestClient.get()
                .uri(AUTH_PATH + "/token/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();

        Map<String, String> cookiesMap = TestCookieUtils.parseCookies(headers);
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

        // Register user assert that the Refresh/Access tokens are present in the response as Cookies
        this.webTestClient.post()
                .uri(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                // expectCookie() returns CookieAssertions and, we can assert on that. Look at DefaultWebTestClient and CookieAssertions
                .expectCookie().exists("ACCESS_TOKEN")
                .expectCookie().exists("REFRESH_TOKEN");
        /*
            An alternative:
                cookiesMap = CookieTestUtils.parseCookieHeaders(headers);
                assertThat(cookiesMap.keySet().stream().anyMatch(key -> key.equals("ACCESS_TOKEN"))).isTrue();
                assertThat(cookiesMap.keySet().stream().anyMatch(key -> key.equals("REFRESH_TOKEN"))).isTrue();
         */
    }

    // We could also combine the two tests(register - login), into one.
    @Test
    void shouldLoginUser() {
        // Get csrf token from response header
        HttpHeaders headers = this.webTestClient.get()
                .uri(AUTH_PATH + "/token/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();

        Map<String, String> cookiesMap = TestCookieUtils.parseCookies(headers);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", userCredentials().get(0).getFirst());
        formData.add("password", userCredentials().get(0).getSecond());

        // Login with user credentials in Spring's endpoint. The user exists in the db from the @SQL script
        this.webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(formData)
                .exchange()
                .expectCookie().exists("ACCESS_TOKEN")
                .expectCookie().exists("REFRESH_TOKEN");
    }

    // refresh()
    @Test
    void shouldRefreshToken() {
        // Get csrf token from response header
        HttpHeaders headers = this.webTestClient.get()
                .uri(AUTH_PATH + "/token/csrf")
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();

        // Login with user credentials in Spring's endpoint. The user exists in the db from the @SQL script
        Map<String, String> cookiesMap = TestCookieUtils.parseCookies(headers);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", userCredentials().get(0).getFirst());
        formData.add("password", userCredentials().get(0).getSecond());

        headers = this.webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(formData)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();

        cookiesMap = TestCookieUtils.parseCookies(headers);
        /*
            We need to hold the value the csrf token has for our next assertions because in the next request when we
            acquire the new Refresh/Access token, we lose the value of the csrf
         */
        String csrfTokenValue = cookiesMap.get("XSRF-TOKEN");
        String refreshTokenValue = cookiesMap.get("REFRESH_TOKEN");
        String accessTokenValue = cookiesMap.get("ACCESS_TOKEN");

        await().pollDelay(Duration.ofSeconds(1)).until(() -> true);

        /*
            We perform 3 assertions
                Case 1: Access and refresh token are set on the response headers
                Case 2: The value of the new refresh token does not match the value of the refresh token that was submitted
                for the request. The old access token and the new one also do not match
                Case 3: Performing a POST request to "/refresh" with the old refresh token leads to 401
         */
        headers = this.webTestClient.post()
                .uri(AUTH_PATH + "/token/refresh")
                .cookie("REFRESH_TOKEN", refreshTokenValue)
                .cookie("XSRF-TOKEN", csrfTokenValue)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .exchange()
                .expectStatus().isOk()
                // expectCookie() returns CookieAssertions and, we can assert on that. Look at DefaultWebTestClient and CookieAssertions
                .expectCookie().exists("ACCESS_TOKEN")
                .expectCookie().exists("REFRESH_TOKEN")
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();


        // At this point we lose the csrf value, since the map from the /refresh endpoint contains only Refresh/Access token
        cookiesMap = TestCookieUtils.parseCookies(headers);
        String newRefreshTokenValue = cookiesMap.get("REFRESH_TOKEN");
        String newAccessTokenValue = cookiesMap.get("ACCESS_TOKEN");
        assertThat(newRefreshTokenValue).isNotEqualTo(refreshTokenValue);
        assertThat(newAccessTokenValue).isNotEqualTo(accessTokenValue);

        this.webTestClient.post()
                .uri(AUTH_PATH + "/token/refresh")
                .cookie("REFRESH_TOKEN", refreshTokenValue)
                .cookie("XSRF-TOKEN", csrfTokenValue)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRevokeAccessToken() {
        // Get csrf token from response header
        HttpHeaders headers = this.webTestClient.get()
                .uri(AUTH_PATH + "/token/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();

        Map<String, String> cookiesMap = TestCookieUtils.parseCookies(headers);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", userCredentials().get(0).getFirst());
        formData.add("password", userCredentials().get(0).getSecond());

        // Login with user credentials in Spring's endpoint. The user exists in the db from the @SQL script
        headers = this.webTestClient.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(formData)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();

        cookiesMap = TestCookieUtils.parseCookies(headers);

        // Revoke the access token by asserting that the response will contain cookies with empty value "" and maxAge 0
        this.webTestClient.post()
                .uri(AUTH_PATH + "/token/revoke")
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .exchange()
                .expectStatus().isOk()
                // expectCookie() returns CookieAssertions and, we can assert on that. Look at DefaultWebTestClient and CookieAssertions
                .expectCookie().valueEquals("ACCESS_TOKEN", "")
                .expectCookie().maxAge("ACCESS_TOKEN", Duration.ZERO)
                .expectCookie().valueEquals("REFRESH_TOKEN", "")
                .expectCookie().maxAge("REFRESH_TOKEN", Duration.ZERO);
    }
}