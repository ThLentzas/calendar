package org.example.google_calendar_clone.user;

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
 */
@AutoConfigureWebTestClient
class UserIT extends AbstractIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String USER_PATH = "/api/v1/user";

    @Test
    @Sql(scripts = "/scripts/INIT_USERS.sql")
    void shouldAddContact() {
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

        String requestBody = """
                {
                    "receiverId": 2
                }
                """;

        this.webTestClient.post()
                .uri(USER_PATH + "/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie("ACCESS_TOKEN", cookiesMap.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk();

        // Login with the receiver and request their pending contact requests
        headers = this.webTestClient.get()
                .uri(AUTH_PATH + "/token/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();
        cookiesMap = TestCookieUtils.parseCookies(headers);

        formData = new LinkedMultiValueMap<>();
        formData.add("username", userCredentials().get(1).getFirst());
        formData.add("password", userCredentials().get(1).getSecond());

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

        this.webTestClient.get()
                .uri(USER_PATH + "/contact-requests")
                .accept(MediaType.APPLICATION_JSON)
                .cookie("ACCESS_TOKEN", cookiesMap.get("ACCESS_TOKEN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                // We can also assert the id, because we know it from the sql file. We could also make a GET request to find
                // the user and assert based on those values.
                .jsonPath("$[0].userProfile.name").isEqualTo("kris.hudson")
                .jsonPath("$[0].status").isEqualTo("PENDING");
    }

    @Test
    @Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldUpdatePendingContactRequest() {
        // Login with the receiver and request their pending contact requests
        HttpHeaders headers = this.webTestClient.get()
                .uri(AUTH_PATH + "/token/csrf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .returnResult(ResponseEntity.class)
                .getResponseHeaders();
        Map<String, String>cookiesMap = TestCookieUtils.parseCookies(headers);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", userCredentials().get(2).getFirst());
        formData.add("password", userCredentials().get(2).getSecond());

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

        String requestBody = """
                {
                    "senderId": 1,
                    "action": "ACCEPT"
                }
                """;

        this.webTestClient.put()
                .uri(USER_PATH + "/contact-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie("ACCESS_TOKEN", cookiesMap.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookiesMap.get("XSRF-TOKEN"))
                .bodyValue(requestBody)
                .exchange()
                .expectStatus()
                .isNoContent();

        // After accepting/rejecting the request, we assert that the pending requests is now only 1 since from the initial
        // sql script user with id 3 had 2 pending contact requests
        this.webTestClient.get()
                .uri(USER_PATH + "/contact-requests")
                .accept(MediaType.APPLICATION_JSON)
                .cookie("ACCESS_TOKEN", cookiesMap.get("ACCESS_TOKEN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].userProfile.name").isEqualTo("clement.gulgowski")
                .jsonPath("$[0].status").isEqualTo("PENDING");
    }
}