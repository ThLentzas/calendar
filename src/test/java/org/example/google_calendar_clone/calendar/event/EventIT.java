package org.example.google_calendar_clone.calendar.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.example.google_calendar_clone.AbstractIntegrationTest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/*
    It is important to generate the dates dynamically so the test will pass the validation of dates being in the
    future or present. This means that we can't assert the exact dates in the response but, we have already tested
    serialization and the business logic that the dates will be computed correctly. What we can assert though is that the
    correct number of upcoming event slots for repeating events will be created, and that they will be sorted in ascending
    order
 */
class EventIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String DAY_EVENT_PATH = "/api/v1/events/day-events";
    private static final String TIME_EVENT_PATH = "/api/v1/events/time-events";

    @Test
    @Sql("/scripts/INIT_USERS.sql")
    void shouldCreateDayEvent() {
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

        String requestBody = String.format("""
                {
                     "name": "Event name",
                     "location": "Location",
                     "description": "Description",
                     "startDate": "%s",
                     "endDate": "%s",
                     "repetitionFrequency": "MONTHLY",
                     "repetitionStep": 3,
                     "monthlyRepetitionType": "SAME_WEEKDAY",
                     "repetitionDuration": "N_REPETITIONS",
                     "repetitionCount": "1"
                }
                """, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        response = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(DAY_EVENT_PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(DAY_EVENT_PATH))
                .extract()
                .response();

        String locationHeader = response.getHeader("Location");
        UUID dayEventId = UUID.fromString(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));

        List<DayEventSlotDTO> dayEventSlots = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_PATH + "/{eventId}", dayEventId)
                .then()
                .statusCode(200)
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the day event slots are in the start/end date. The remaining properties upon creating
            an event will be the same for all. We can not assert on the stat/end date but, we can make sure that they
            are sorted based on the start date and the list of event slots has the expected size based on the repetition
            values provided
         */
        assertThat(dayEventSlots).hasSize(2)
                .allMatch(slot -> slot.getName().equals("Event name")
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getOrganizer().equals("ellyn.roberts")
                        && slot.getGuestEmails().equals(Collections.emptySet())
                        && slot.getDayEventId().equals(dayEventId))
                .isSortedAccordingTo(Comparator.comparing(DayEventSlotDTO::getStartDate));
    }

    @Test
    @Sql("/scripts/INIT_USERS.sql")
    void shouldCreateTimeEvent() {
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

        String requestBody = String.format("""
                {
                    "name": "Event name",
                    "location": "Location",
                    "description": "Description",
                    "startTime": "%s",
                    "endTime": "%s",
                    "startTimeZoneId": "Europe/London",
                    "endTimeZoneId": "Europe/London",
                    "repetitionFrequency": "WEEKLY",
                    "repetitionStep": 2,
                    "repetitionDuration": "N_REPETITIONS",
                    "repetitionCount": 5
                }
                """, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        // Create the time event
        response = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(TIME_EVENT_PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(TIME_EVENT_PATH))
                .extract()
                .response();

        String locationHeader = response.getHeader("Location");
        UUID timeEventId = UUID.fromString(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));

        // Retrieve the TimeEventSlots for the created TimeEvent
        List<TimeEventSlotDTO> timeEventSlots = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_PATH + "/{eventId}", timeEventId)
                .then()
                .statusCode(200)
                .log().all()
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the time event slots are in the start/end time. The remaining properties upon creating
            an event will be the same. We can not assert on the stat/end time but, we can make sure that they
            are sorted based on the start time and the list of event slots has the expected size based on the repetition
            values provided
         */
        assertThat(timeEventSlots).hasSize(6)
                .allMatch(slot -> slot.getName().equals("Event name")
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getOrganizer().equals("ellyn.roberts")
                        && slot.getGuestEmails().equals(Collections.emptySet())
                        && slot.getStartTimeZoneId().equals(ZoneId.of("Europe/London"))
                        && slot.getEndTimeZoneId().equals(ZoneId.of("Europe/London"))
                        && slot.getTimeEventId().equals(timeEventId))
                .isSortedAccordingTo(Comparator.comparing(TimeEventSlotDTO::getStartTime));
    }
}
