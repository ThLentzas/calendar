package org.example.calendar.event.slot.time.projection;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class TimeEventSlotPublicProjectionJsonTest {
    @Autowired
    private JacksonTester<TimeEventSlotPublicProjection> jacksonTester;
    @Autowired
    private JacksonTester<List<TimeEventSlotPublicProjection>> listJacksonTester;

    @Test
    void shouldSerializeTimeEventSlot() throws IOException {
        TimeEventSlotPublicProjection expected = TimeEventSlotPublicProjection.builder()
                .id(UUID.fromString("e431687e-7251-4ac6-b797-c107064af135"))
                .title("Event title")
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .startTime(LocalDateTime.parse("2024-10-15T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .eventId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"))
                .build();

        String json = """
                {
                    "id": "e431687e-7251-4ac6-b797-c107064af135",
                    "title": "Event title",
                    "location": "Location",
                    "description": "Description",
                    "organizer": "ellyn.roberts",
                    "guestEmails": [],
                    "startTime": "2024-10-15T10:00:00",
                    "endTime": "2024-10-15T15:00:00",
                    "startTimeZoneId": "Europe/London",
                    "endTimeZoneId": "Europe/London",
                    "eventId": "6b9b32f2-3c2a-4420-9d52-781c09f320ce"
                }
                """;

        JsonContent<TimeEventSlotPublicProjection> actual = this.jacksonTester.write(expected);

        assertThat(actual).isEqualToJson(json);
    }

    @Test
    void shouldSerializeTimeEventSlotList() throws IOException {
        TimeEventSlotPublicProjection projection1 = TimeEventSlotPublicProjection.builder()
                .id(UUID.fromString("e431687e-7251-4ac6-b797-c107064af135"))
                .title("Event title")
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .startTime(LocalDateTime.parse("2024-10-15T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .eventId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"))
                .build();

        TimeEventSlotPublicProjection projection2 = TimeEventSlotPublicProjection.builder()
                .id(UUID.fromString("9aeed400-ad9d-462c-bfb7-c5c307161a8d"))
                .title("Event title")
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .startTime(LocalDateTime.parse("2024-10-29T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-29T14:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .eventId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"))
                .build();

        List<TimeEventSlotPublicProjection> projections = List.of(projection1, projection2);

        String json = """
                [
                    {
                        "id": "e431687e-7251-4ac6-b797-c107064af135",
                        "title": "Event title",
                        "location": "Location",
                        "description": "Description",
                        "organizer": "ellyn.roberts",
                        "guestEmails": [],
                        "startTime": "2024-10-15T10:00:00",
                        "endTime": "2024-10-15T15:00:00",
                        "startTimeZoneId": "Europe/London",
                        "endTimeZoneId": "Europe/London",
                        "eventId": "6b9b32f2-3c2a-4420-9d52-781c09f320ce"
                    },
                    {
                        "id": "9aeed400-ad9d-462c-bfb7-c5c307161a8d",
                        "title": "Event title",
                        "location": "Location",
                        "description": "Description",
                        "organizer": "ellyn.roberts",
                        "guestEmails": [],
                        "startTime": "2024-10-29T10:00:00",
                        "endTime": "2024-10-29T14:00:00",
                        "startTimeZoneId": "Europe/London",
                        "endTimeZoneId": "Europe/London",
                        "eventId": "6b9b32f2-3c2a-4420-9d52-781c09f320ce"
                    }
                ]
                """;

        JsonContent<List<TimeEventSlotPublicProjection>> actual = this.listJacksonTester.write(projections);

        assertThat(actual).isEqualToJson(json);
    }
}