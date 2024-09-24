package org.example.calendar.event.time.dto;

import org.example.calendar.event.time.dto.TimeEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class TimeEventRequestJsonTest {
    @Autowired
    private JacksonTester<TimeEventRequest> jacksonTester;

    @Test
    void shouldDeserializeTimeEventRequest() throws IOException {
        String json = """
                {
                     "title": "Event title",
                     "location": "Location",
                     "description": "Description",
                     "startTime": "2024-10-11T10:00",
                     "endTime": "2024-10-15T15:00",
                     "startTimeZoneId": "Europe/London",
                     "endTimeZoneId": "Europe/London",
                     "recurrenceFrequency": "DAILY",
                     "recurrenceStep": 2,
                     "recurrenceDuration": "N_OCCURRENCES",
                     "numberOfOccurrences": 5
                }
                """;

        TimeEventRequest expected = TimeEventRequest.builder()
                .title("Event title")
                .location("Location")
                .description("Description")
                .startTime(LocalDateTime.parse("2024-10-11T10:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(2)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(5)
                .build();

        TimeEventRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }

}
