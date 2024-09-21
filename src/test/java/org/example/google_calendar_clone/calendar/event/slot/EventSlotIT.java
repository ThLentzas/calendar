package org.example.google_calendar_clone.calendar.event.slot;

import org.example.google_calendar_clone.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;
import java.util.UUID;

class EventSlotIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String DAY_EVENT_SLOT_PATH = "/api/v1/event-slots/day-event-slots";
    private static final String TIME_EVENT_SLOT_PATH = "/api/v1/event-slots/time-event-slots";

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldInviteGuestsForDayEventSlot() {
        String guestEmail = FAKER.internet().emailAddress();
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
                .log().all()
                .body("id", equalTo(dayEventSlotId.toString()))
                .body("title", equalTo("Event title"))
                .body("location", equalTo("Location"))
                .body("organizer", equalTo("ellyn.roberts"))
                .body("guestEmails", hasItems(
                        "ericka.ankunding@hotmail.com",
                        guestEmail
                ))
                .body("startDate", equalTo("2024-10-29"))
                .body("endDate", equalTo("2024-10-30"))
                .body("dayEventId", equalTo("6b9b32f2-3c2a-4420-9d52-781c09f320ce"));
    }

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldInviteGuestsForTimeEventSlot() {
        String guestEmail = FAKER.internet().emailAddress();
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
                .statusCode(204)
                .extract()
                .response();

        // GET api/v1/events/time-event-slots/{slotId} is tested as well here to make sure the new guest is added to the list
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_SLOT_PATH + "/{slotId}", timeEventSlotId)
                .then()
                .statusCode(200)
                .log().all()
                .body("id", equalTo(timeEventSlotId.toString()))
                .body("title", equalTo("Event title"))
                .body("startTime", equalTo("2024-10-11T10:00:00"))
                .body("endTime", equalTo("2024-10-15T15:00:00"))
                .body("startTimeZoneId", equalTo("Europe/London"))
                .body("endTimeZoneId", equalTo("Europe/London"))
                .body("location", equalTo("Location"))
                .body("organizer", equalTo("kris.hudson"))
                .body("guestEmails", hasItems(
                        "ericka.ankunding@hotmail.com",
                        guestEmail
                ))
                .body("timeEventId", equalTo("0c9d6398-a6de-47f0-8328-04a2f3c0511c"));
    }
}