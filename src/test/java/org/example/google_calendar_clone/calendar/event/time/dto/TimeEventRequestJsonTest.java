package org.example.google_calendar_clone.calendar.event.time.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class TimeEventRequestJsonTest {
    @Autowired
    private JacksonTester<TimeEventRequest> jacksonTester;

    @Test
    void shouldDeserializeTimeEventRequest() throws IOException {
        String json = """
                {
                     "name": "Event name",
                     "location": "Location",
                     "description": "Description",
                     "startTime": "2024-10-11T10:00",
                     "endTime": "2024-10-15T15:00",
                     "startTimeZoneId": "Europe/London",
                     "endTimeZoneId": "Europe/London",
                     "repetitionFrequency": "WEEKLY",
                     "repetitionStep": 2,
                     "repetitionDuration": "N_REPETITIONS",
                     "repetitionCount": 5
                }
                """;

        TimeEventRequest expected = TimeEventRequest.builder()
                .name("Event name")
                .location("Location")
                .description("Description")
                .startTime(LocalDateTime.parse("2024-10-11T10:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(5)
                .build();

        TimeEventRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }

}
