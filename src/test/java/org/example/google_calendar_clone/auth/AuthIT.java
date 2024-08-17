package org.example.google_calendar_clone.auth;

import org.example.google_calendar_clone.AbstractIntegrationTest;
import org.example.google_calendar_clone.util.CookieTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
class AuthIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final Faker FAKER = new Faker();
    private static final String AUTH_PATH = "/api/v1/auth";

    // register()
    @Test
    void shouldRegisterUser() {
        EntityExchangeResult<byte[]> response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        Map<String, String> cookiesMap = CookieTestUtils.parseCookieHeaders(response);
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

        this.webTestClient.post()
                .uri(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectCookie().exists("ACCESS_TOKEN")
                .expectCookie().exists("REFRESH_TOKEN");

        /*
            An alternative:
                cookiesMap = CookieTestUtils.parseCookieHeaders(response);
                assertThat(cookiesMap.keySet().stream().anyMatch(key -> key.equals("ACCESS_TOKEN"))).isTrue();
                assertThat(cookiesMap.keySet().stream().anyMatch(key -> key.equals("REFRESH_TOKEN"))).isTrue();
         */
    }

    // refresh()
    @Test
    void shouldRefreshToken() {
        EntityExchangeResult<byte[]> response = this.webTestClient.get()
                .uri(AUTH_PATH + "/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody()
                .returnResult();

        Map<String, String> cookiesMap = CookieTestUtils.parseCookieHeaders(response);
        String csrfTokenValue = cookiesMap.get("XSRF-TOKEN");
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

        response = this.webTestClient.post()
                .uri(AUTH_PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie("XSRF-TOKEN", csrfTokenValue)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .bodyValue(requestBody)
                .exchange()
                .expectBody().returnResult();

        // cookiesMap is updated and, we loose the value for csrf, that's why we introduced the csrfTokenValue variable
        cookiesMap = CookieTestUtils.parseCookieHeaders(response);
        String refreshTokenValue = cookiesMap.get("REFRESH_TOKEN");

        /*
            We perform 3 assertions
                Case 1: Access and refresh token are set on the response headers
                Case 2: The value of the new refresh token does not match the value of the refresh token that was submitted
                for the request
                Case 3: Performing a POST request to "/refresh" with the old refresh token leads to 401
         */
        response = this.webTestClient.post()
                .uri(AUTH_PATH + "/refresh")
                .cookie("REFRESH_TOKEN", refreshTokenValue)
                .cookie("XSRF-TOKEN", csrfTokenValue)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .exchange()
                .expectStatus().isOk()
                .expectCookie().exists("ACCESS_TOKEN")
                .expectCookie().exists("REFRESH_TOKEN")
                .expectBody().returnResult();

        cookiesMap = CookieTestUtils.parseCookieHeaders(response);
        String newRefreshTokenValue = cookiesMap.get("REFRESH_TOKEN");

        assertThat(newRefreshTokenValue).isNotEqualTo(refreshTokenValue);
        this.webTestClient.post()
                .uri(AUTH_PATH + "/refresh")
                .cookie("REFRESH_TOKEN", refreshTokenValue)
                .cookie("XSRF-TOKEN", csrfTokenValue)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}