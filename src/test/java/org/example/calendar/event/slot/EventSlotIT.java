package org.example.calendar.event.slot;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static io.restassured.RestAssured.given;

import org.example.calendar.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

/*
    @Sql(scripts = "/scripts/INIT_USERS.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
    Using the above at class level and the following method in the abstract class will not work.

    @AfterEach
    void clear() {
        this.userRepository.deleteAll();
    }

    Our script of adding users/events is executed once for the entire class, but we delete them after each test, which causes
    the remaining tests to fail, since there aren't any users.

    I decided to keep the @AfterEach and pass the script in every test. Apart from providing a clean state it also
    deletes all the other records we might have that have the user_id ON DELETE CASCADE

    @Sql(scripts = "/scripts/INIT_USERS.sql")
    class EventSlotIT extends AbstractIntegrationTest -> Won't work either without executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
    because every method level @Sql script will overwrite it.

    After removing the credentials for sending emails, and replaced them with placeholders those tests will fail because
    it will try to find the values, and they will not be present. The error message does not give any information
        ${MAIL_USERNAME}, ${MAIL_PASSWORD}
    The EventIT test pass because the Test Profile is Active and replaces those values
 */
class EventSlotIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String DAY_EVENT_SLOT_PATH = "/api/v1/event-slots/day-event-slots";
    private static final String TIME_EVENT_SLOT_PATH = "/api/v1/event-slots/time-event-slots";

    //inviteGuestToDayEventSlot()
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldInviteGuestsToDayEventSlot() {
        String guestEmail = "keenan.jaskolski@hotmail.com";
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
        cookies = response.getCookies();
        // Known from the sql script
        UUID dayEventSlotId = UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa");
        String requestBody = String.format("""
                {
                    "guestEmails": ["%s"]
                }
                """, guestEmail);

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", dayEventSlotId)
                .then()
                .statusCode(204)
                .extract()
                .response();

        // GET api/v1/events/day-event-slots/{slotId} is tested as well here to make sure the new guest is added to the list
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_SLOT_PATH + "/{slotId}", dayEventSlotId)
                .then()
                .statusCode(200)
                .body("id", equalTo(dayEventSlotId.toString()))
                .body("title", equalTo("Event title"))
                .body("location", equalTo("Location"))
                .body("organizer", equalTo("ellyn.roberts"))
                // order matters for the emails (hasItems ignores order). There is also containsInRelativeOrder()
                .body("guestEmails", contains("ericka.ankunding@hotmail.com", guestEmail))
                .body("startDate", equalTo("2024-10-29"))
                .body("endDate", equalTo("2024-10-30"))
                .body("dayEventId", equalTo("6b9b32f2-3c2a-4420-9d52-781c09f320ce"));
    }

    //updateDayEventSlot()
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldUpdateDayEventSlot() {
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
        String guestEmail = FAKER.internet().emailAddress();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(3);
        UUID slotId = UUID.fromString("e2985eda-5c5a-40a0-851e-6dc088081afa");

        String requestBody = String.format("""
                {
                     "title": "Title",
                     "location": "New location",
                     "description": "New description",
                     "guestEmails": ["%s"],
                     "startDate": "%s",
                     "endDate": "%s"
                }
                """, guestEmail, startDate, endDate);

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(204);

        // GET api/v1/event-slots/day-event-slots/{slotId} is tested as well here to make sure the new guest is added to the list
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(200)
                .body("id", equalTo(slotId.toString()))
                .body("title", equalTo("Title"))
                .body("startDate", equalTo(startDate.toString()))
                .body("endDate", equalTo(endDate.toString()))
                .body("location", equalTo("New location"))
                .body("description", equalTo("New description"))
                .body("organizer", equalTo("clement.gulgowski"))
                .body("guestEmails", hasItems(guestEmail))
                .body("dayEventId", equalTo("4472d36c-2051-40e3-a2cf-00c6497807b5"));
    }

    // deleteDayEventSlot()
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldDeleteDayEventSlot() {
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
        UUID slotId = UUID.fromString("e2985eda-5c5a-40a0-851e-6dc088081afa");

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .delete(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(204);

        // GET api/v1/event-slots/day-event-slots/{slotId} is tested
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(404);
    }

    // inviteGuestsToDayEventSlot()
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldInviteGuestsToTimeEventSlot() {
        String guestEmail = "keenan.jaskolski@hotmail.com";
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
        cookies = response.getCookies();
        // Known from the sql script
        UUID timeEventSlotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String requestBody = String.format("""
                {
                    "guestEmails": ["%s"]
                }
                """, guestEmail);

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", timeEventSlotId)
                .then()
                .statusCode(204);

        // GET api/v1/event-slots/time-event-slots/{slotId} is tested as well here to make sure the new guest is added to the list
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_SLOT_PATH + "/{slotId}", timeEventSlotId)
                .then()
                .statusCode(200)
                .body("id", equalTo(timeEventSlotId.toString()))
                .body("title", equalTo("Event title"))
                .body("startTime", equalTo("2024-10-11T10:00:00"))
                .body("endTime", equalTo("2024-10-15T15:00:00"))
                .body("startTimeZoneId", equalTo("Europe/London"))
                .body("endTimeZoneId", equalTo("Europe/London"))
                .body("location", equalTo("Location"))
                .body("organizer", equalTo("kris.hudson"))
                // order matters for the emails (hasItems ignores order). There is also containsInRelativeOrder()
                .body("guestEmails", contains("ericka.ankunding@hotmail.com", guestEmail))
                .body("timeEventId", equalTo("0c9d6398-a6de-47f0-8328-04a2f3c0511c"));
    }

    // updateTimeEventSlot
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldUpdateTimeEventSlot() {
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
        cookies = response.getCookies();
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");

        String requestBody = """
                {
                    "title": "Title",
                    "location": "New location",
                    "description": "New description"
                }
                """;

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(204);

        // GET api/v1/event-slots/day-event-slots/{slotId} is tested as well here to make sure the new guest is added to the list
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(200)
                .body("id", equalTo(slotId.toString()))
                .body("title", equalTo("Title"))
                .body("location", equalTo("New location"))
                .body("description", equalTo("New description"));
    }

    // deleteTimeEventSlot
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldDeleteTimeEventSlot() {
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
        cookies = response.getCookies();
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .delete(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(204);

        // GET api/v1/event-slots/day-event-slots/{slotId} is tested
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                .then()
                .statusCode(404);
    }
}