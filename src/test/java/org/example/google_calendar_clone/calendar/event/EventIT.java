package org.example.google_calendar_clone.calendar.event;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static io.restassured.RestAssured.given;

import org.example.google_calendar_clone.AbstractIntegrationTest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import net.datafaker.Faker;

/*
    It is important to generate the dates dynamically so the test will pass the validation of dates being in the
    future or present. This means that we can't assert the exact dates in the response but, we have already tested
    serialization and the business logic that the dates will be computed correctly. What we can assert though is that the
    correct number of upcoming event slots for repeating events will be created, and that they will be sorted in ascending
    order

    Time assertions in the tests below, are in the timezone provided by the user. In the sql, scripts are in UTC, but
    we assert on the local time based on the timezone
 */
@ActiveProfiles(profiles = "test")
class EventIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String EVENT_PATH = "/api/v1/events";
    private static final String DAY_EVENT_PATH = EVENT_PATH + "/day-events";
    private static final String TIME_EVENT_PATH = EVENT_PATH + "/time-events";
    private static final String DAY_EVENT_SLOT_PATH = EVENT_PATH + "/day-event-slots";
    private static final String TIME_EVENT_SLOT_PATH = EVENT_PATH + "/time-event-slots";
    private static final Faker FAKER = new Faker();

    @Test
    @Sql("/scripts/INIT_USERS.sql")
    void shouldCreateDayEvent() throws MessagingException {
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
        // The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
        cookies = response.getCookies();

        String requestBody = String.format("""
                {
                     "name": "Event name",
                     "location": "Location",
                     "description": "Description",
                     "guestEmails": ["%s"],
                     "startDate": "%s",
                     "endDate": "%s",
                     "repetitionFrequency": "MONTHLY",
                     "repetitionStep": 3,
                     "monthlyRepetitionType": "SAME_WEEKDAY",
                     "repetitionDuration": "N_REPETITIONS",
                     "repetitionOccurrences": 2
                }
                """, guestEmail, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

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
                .get(DAY_EVENT_PATH + "/{eventId}/day-event-slots", dayEventId)
                .then().statusCode(200)
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
                        && slot.getGuestEmails().equals(Set.of(guestEmail))
                        && slot.getDayEventId().equals(dayEventId))
                .isSortedAccordingTo(Comparator.comparing(DayEventSlotDTO::getStartDate));

        /*
            Asserting on the invitation email(recipient, subject)
            Sending the email is done Async so, we have to wait before we assert

            Setting the body of the email correctly is already tested in ThymeleafServiceTest and EmailUtilsTest
         */
        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);

        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];

        assertThat(messages).hasSize(1);
        assertThat(message.getAllRecipients()[0]).hasToString(guestEmail);
        assertThat(message.getSubject()).isEqualTo("Invitation");
    }

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldDeleteDayEvent() {
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
        // The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
        cookies = response.getCookies();
        // Known from the sql script
        UUID dayEventId = UUID.fromString("4472d36c-2051-40e3-a2cf-00c6497807b5");

        // Delete the event
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .delete(DAY_EVENT_PATH + "/{eventId}", dayEventId)
                .then()
                .statusCode(204);

        // GET request should result in 404
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_PATH + "/{eventId}/day-event-slots", dayEventId)
                .then()
                .statusCode(404);
    }

    @Test
    @Sql("/scripts/INIT_USERS.sql")
    void shouldCreateTimeEvent() throws MessagingException {
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
        // The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
        cookies = response.getCookies();

        /*
            Adjusts startTime to the closest upcoming Friday. If startDate is already a Friday, it remains unchanged.

            LocalDateTime.now().with(DayOfWeek.FRIDAY): If today is Wednesday, September 11, 2024, and the current time
            is 10:00 AM, calling LocalDateTime.now().with(DayOfWeek.FRIDAY) will return Friday, September 13, 2024,
            10:00 AM.

            LocalDateTime.now().plusMinutes(30).with(DayOfWeek.FRIDAY): If today is Wednesday, September 11, 2024,
            and the current time is 10:00 AM, calling LocalDateTime.now().plusMinutes(30).with(DayOfWeek.FRIDAY) will
            return Friday, September 13, 2024, 10:30 AM.

            We follow this logic because we want to pass the correct day in the weeklyRecurrenceDays(), otherwise we
            could follow the same logic as shouldCreateDayEvent()
        */
        String requestBody = String.format("""
                        {
                            "name": "Event name",
                            "location": "Location",
                            "description": "Description",
                            "guestEmails": ["%s"],
                            "startTime": "%s",
                            "endTime": "%s",
                            "startTimeZoneId": "Europe/London",
                            "endTimeZoneId": "Europe/London",
                            "repetitionFrequency": "WEEKLY",
                            "repetitionStep": 2,
                            "weeklyRecurrenceDays": ["THURSDAY", "SATURDAY"],
                            "repetitionDuration": "N_REPETITIONS",
                            "repetitionOccurrences": 5
                        }
                        """,
                guestEmail,
                LocalDateTime.now().with(DayOfWeek.THURSDAY).plusWeeks(1),
                LocalDateTime.now().with(DayOfWeek.THURSDAY).plusWeeks(1).plusMinutes(30));

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
                .get(TIME_EVENT_PATH + "/{eventId}/time-event-slots", timeEventId)
                .then()
                .statusCode(200)
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the time event slots are in the start/end time. The remaining properties upon creating
            an event will be the same. We can not assert on the stat/end time but, we can make sure that they
            are sorted based on the start time and the list of event slots has the expected size based on the repetition
            values provided
         */
        assertThat(timeEventSlots).hasSize(5)
                .allMatch(slot -> slot.getName().equals("Event name")
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getOrganizer().equals("ellyn.roberts")
                        && slot.getGuestEmails().equals(Set.of(guestEmail))
                        && slot.getStartTimeZoneId().equals(ZoneId.of("Europe/London"))
                        && slot.getEndTimeZoneId().equals(ZoneId.of("Europe/London"))
                        && slot.getTimeEventId().equals(timeEventId))
                .isSortedAccordingTo(Comparator.comparing(TimeEventSlotDTO::getStartTime));
        /*
            Asserting on the invitation email(recipient, subject)
            Sending the email is done Async so, we have to wait before we assert

            Setting the body of the email correctly is already tested in ThymeleafServiceTest and EmailUtilsTest
         */
        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);

        MimeMessage[] messages = greenMail.getReceivedMessages();
        MimeMessage message = messages[0];

        assertThat(messages).hasSize(1);
        assertThat(message.getAllRecipients()[0]).hasToString(guestEmail);
        assertThat(message.getSubject()).isEqualTo("Invitation");
    }

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldDeleteTimeEvent() {
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
        // Known from the sql script
        UUID timeEventId = UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c");

        // Delete the event
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .when()
                .delete(TIME_EVENT_PATH + "/{eventId}", timeEventId)
                .then()
                .statusCode(204);

        // GET request should result in 404
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_PATH + "/{eventId}/time-event-slots", timeEventId)
                .then()
                .statusCode(404);
    }

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldFindEventsInRange() {
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
        // The response will contain the new XSRF-TOKEN from the CsrfAuthenticationStrategy
        cookies = response.getCookies();

        /*
            There are 3 events that fall in the provided date range. 2 TimeEvents and 1 DayEvent. The user that made
            the request is the organizer of the DayEvent, and is invited to the other 2.
         */
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .queryParam("start", "2024-10-10")
                .queryParam("end", "2024-10-28")
                .when()
                .get(EVENT_PATH)
                .then()
                .statusCode(200)
                .body("", hasSize(3))
                // In the script the time is: '2024-10-11T09:00:00' (UTC) but since DST is active at that time for the
                // startTimeZoneId we adjust it when we return to the user
                .body("[0].startTime", equalTo("2024-10-11T10:00:00"))
                .body("[0].organizer", equalTo("kris.hudson"))
                // Guest
                .body("[0].guestEmails[0]", equalTo("ericka.ankunding@hotmail.com"))
                // Organizer
                .body("[1].startDate", equalTo("2024-10-12"))
                .body("[1].organizer", equalTo("clement.gulgowski"))
                .body("[2].startTime", equalTo("2024-10-25T10:00:00"))
                .body("[2].organizer", equalTo("kris.hudson"))
                // Guest
                .body("[2].guestEmails[0]", equalTo("ericka.ankunding@hotmail.com"));
    }

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
                .body("name", equalTo("Event name"))
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
                .body("name", equalTo("Event name"))
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
