package org.example.calendar.event;

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
import static org.hamcrest.Matchers.hasSize;
import static io.restassured.RestAssured.given;

import org.example.calendar.AbstractIntegrationTest;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
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
    correct number of upcoming event slots for recurring events will be created, and that they will be sorted in ascending
    order

    Time assertions in the tests below, are in the timezone provided by the user. In the sql, scripts are in UTC, but
    we assert on the local time based on the timezone

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
    class EventIT extends AbstractIntegrationTest -> Won't work either without executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
    because every method level @Sql script will overwrite it.
 */
@ActiveProfiles(profiles = "test")
class EventIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String EVENT_PATH = "/api/v1/events";
    private static final String DAY_EVENT_PATH = EVENT_PATH + "/day-events";
    private static final String TIME_EVENT_PATH = EVENT_PATH + "/time-events";
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
                     "title": "Event title",
                     "location": "Location",
                     "description": "Description",
                     "guestEmails": ["%s"],
                     "startDate": "%s",
                     "endDate": "%s",
                     "recurrenceFrequency": "MONTHLY",
                     "recurrenceStep": 3,
                     "monthlyRecurrenceType": "SAME_WEEKDAY",
                     "recurrenceDuration": "N_OCCURRENCES",
                     "numberOfOccurrences": 2
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

        List<DayEventSlotPublicProjection> dayEventSlots = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_PATH + "/{eventId}", dayEventId)
                .then().statusCode(200)
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the day event slots are in the start/end date. The remaining properties upon creating
            an event will be the same for all. We can not assert on the stat/end date but, we can make sure that they
            are sorted based on the start date and the list of event slots has the expected size based on the recurrence
            values provided
         */
        assertThat(dayEventSlots).hasSize(2)
                .allMatch(slot -> slot.getTitle().equals("Event title")
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getOrganizer().equals("ellyn.roberts")
                        && slot.getGuestEmails().equals(Set.of(guestEmail))
                        && slot.getEventId().equals(dayEventId))
                .isSortedAccordingTo(Comparator.comparing(DayEventSlotPublicProjection::getStartDate));

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
    void shouldUpdateDayEvent() {
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
                     "title": "Updated title",
                     "location": "Updated location",
                     "description": "Updated description",
                     "startDate": "%s",
                     "endDate": "%s",
                     "recurrenceFrequency": "DAILY",
                     "recurrenceStep": 4,
                     "recurrenceDuration": "N_OCCURRENCES",
                     "numberOfOccurrences": 3
                }
                """, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        UUID eventId = UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce");

        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(DAY_EVENT_PATH + "/{eventId}", eventId)
                .then()
                .statusCode(204);

        List<DayEventSlotPublicProjection> dayEventSlots = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_PATH + "/{eventId}", eventId)
                .then().statusCode(200)
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the day event slots are in the start/end date. The remaining properties upon creating
            an event will be the same for all. We can not assert on the stat/end date but, we can make sure that they
            are sorted based on the start date and the list of event slots has the expected size based on the recurrence
            values provided.

            With the frequency details that we passed in the update request we assert on the new values.
         */
        assertThat(dayEventSlots).hasSize(3)
                .allMatch(slot -> slot.getTitle().equals("Updated title")
                        && slot.getLocation().equals("Updated location")
                        && slot.getDescription().equals("Updated description")
                        && slot.getOrganizer().equals("ellyn.roberts"))
                .isSortedAccordingTo(Comparator.comparing(DayEventSlotPublicProjection::getStartDate));
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

        // GET request should result in an empty list, since we return event slots for the given event
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(DAY_EVENT_PATH + "/{eventId}", dayEventId)
                .then()
                .body("", hasSize(0));
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
                            "title": "Event title",
                            "location": "Location",
                            "description": "Description",
                            "guestEmails": ["%s"],
                            "startTime": "%s",
                            "endTime": "%s",
                            "startTimeZoneId": "Europe/London",
                            "endTimeZoneId": "Europe/London",
                            "recurrenceFrequency": "WEEKLY",
                            "recurrenceStep": 2,
                            "weeklyRecurrenceDays": ["THURSDAY", "SATURDAY"],
                            "recurrenceDuration": "N_OCCURRENCES",
                            "numberOfOccurrences": 5
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
        UUID eventId = UUID.fromString(locationHeader.substring(locationHeader.lastIndexOf('/') + 1));

        // Retrieve the TimeEventSlots for the created TimeEvent
        List<TimeEventSlotPublicProjection> timeEventSlots = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_PATH + "/{eventId}", eventId)
                .then()
                .statusCode(200)
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the time event slots are in the start/end time. The remaining properties upon creating
            an event will be the same. We can not assert on the stat/end time but, we can make sure that they
            are sorted based on the start time and the list of event slots has the expected size based on the recurrence
            values provided
         */
        assertThat(timeEventSlots).hasSize(5)
                .allMatch(slot -> slot.getTitle().equals("Event title")
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getOrganizer().equals("ellyn.roberts")
                        && slot.getGuestEmails().equals(Set.of(guestEmail))
                        && slot.getStartTimeZoneId().equals(ZoneId.of("Europe/London"))
                        && slot.getEndTimeZoneId().equals(ZoneId.of("Europe/London"))
                        && slot.getEventId().equals(eventId))
                .isSortedAccordingTo(Comparator.comparing(TimeEventSlotPublicProjection::getStartTime));
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
    void shouldUpdateTimeEvent() {
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

        String requestBody = String.format("""
                {
                     "startTime": "%s",
                     "startTimeZoneId": "Europe/London",
                     "endTime": "%s",
                     "endTimeZoneId": "Europe/London",
                     "title": "Updated title",
                     "location": "Updated location",
                     "description": "Updated description",
                     "recurrenceFrequency": "WEEKLY",
                     "recurrenceStep": 2,
                     "weeklyRecurrenceDays": ["FRIDAY"],
                     "recurrenceDuration": "N_OCCURRENCES",
                     "numberOfOccurrences": 3
                }
                """, LocalDateTime.now().with(DayOfWeek.FRIDAY).plusWeeks(1), LocalDateTime.now().with(DayOfWeek.FRIDAY).plusWeeks(1).plusMinutes(30));
        UUID eventId = UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c");
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .cookie("XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"))
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put(TIME_EVENT_PATH + "/{eventId}", eventId)
                .then()
                .statusCode(204);

        List<TimeEventSlotPublicProjection> eventSlots = given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_PATH + "/{eventId}", eventId)
                .then().statusCode(200)
                .extract()
                .response().as(new TypeRef<>() {
                });

        /*
            The differences between the day event slots are in the start/end date. The remaining properties upon creating
            an event will be the same for all. We can not assert on the stat/end date but, we can make sure that they
            are sorted based on the start date and the list of event slots has the expected size based on the recurrence
            values provided.

            With the frequency details that we passed in the update request we assert on the new values.
         */
        assertThat(eventSlots).hasSize(3)
                .allMatch(slot -> slot.getTitle().equals("Updated title")
                        && slot.getLocation().equals("Updated location")
                        && slot.getDescription().equals("Updated description")
                        && slot.getOrganizer().equals("kris.hudson"))
                .isSortedAccordingTo(Comparator.comparing(TimeEventSlotPublicProjection::getStartTime));
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

        // GET request should result in an empty list, since we return event slots for the given event
        given()
                .cookie("ACCESS_TOKEN", cookies.get("ACCESS_TOKEN"))
                .accept(ContentType.JSON)
                .when()
                .get(TIME_EVENT_PATH + "/{eventId}", timeEventId)
                .then()
                .body("", hasSize(0));
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
                // The timezones are defaulted to "" which results in UTC
                .queryParam("start", "2024-10-10")
                .queryParam("end", "2024-10-28")
                .when()
                .get(EVENT_PATH)
                .then()
                .statusCode(200)
                .body("", hasSize(2))
                .body("[0].startDate", equalTo("2024-10-12"))
                .body("[0].organizer", equalTo("clement.gulgowski"))
                // In the script the time is: '2024-10-11T09:00:00' (UTC) but since DST is active at that time for the
                // startTimeZoneId we adjust it when we return to the user
                .body("[1].startTime", equalTo("2024-10-15T10:00:00"))
                .body("[1].organizer", equalTo("kris.hudson"))
                .body("[1].guestEmails[0]", equalTo("ericka.ankunding@hotmail.com"));
    }
}
