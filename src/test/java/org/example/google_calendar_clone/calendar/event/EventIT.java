package org.example.google_calendar_clone.calendar.event;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.example.google_calendar_clone.AbstractIntegrationTest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

@Sql("/scripts/INIT_USERS.sql")
class EventIT extends AbstractIntegrationTest {
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String DAY_EVENT_PATH = "/api/v1/events/day-events";

    @Test
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

        String requestBody = """
                {
                     "name": "Event name",
                     "location": "Location",
                     "description": "Description",
                     "startDate": "2024-10-11",
                     "endDate": "2024-10-15",
                     "repetitionFrequency": "MONTHLY",
                     "repetitionStep": 3,
                     "monthlyRepetitionType": "SAME_WEEKDAY",
                     "repetitionDuration": "N_REPETITIONS",
                     "repetitionCount": "1"
                }
                """;

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
                .log().all()
                .extract()
                .response().as(new TypeRef<>() {
                });

        assertThat(dayEventSlots).hasSize(2)
                .anyMatch(slot -> slot.getName().equals("Event name")
                        && slot.getStartDate().equals(LocalDate.parse("2024-10-11"))
                        && slot.getEndDate().equals(LocalDate.parse("2024-10-15"))
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getGuestEmails().equals(Collections.emptySet()))
                .anyMatch(slot -> slot.getName().equals("Event name")
                        && slot.getStartDate().equals(LocalDate.parse("2025-01-10"))
                        && slot.getEndDate().equals(LocalDate.parse("2025-01-14"))
                        && slot.getLocation().equals("Location")
                        && slot.getDescription().equals("Description")
                        && slot.getGuestEmails().equals(Collections.emptySet()))
                .isSortedAccordingTo(Comparator.comparing(DayEventSlotDTO::getStartDate));

    }
}
