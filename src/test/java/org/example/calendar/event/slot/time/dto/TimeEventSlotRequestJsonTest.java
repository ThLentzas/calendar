package org.example.calendar.event.slot.time.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class TimeEventSlotRequestJsonTest {
    @Autowired
    private JacksonTester<TimeEventSlotRequest> jacksonTester;

    @Test
    void shouldDeserializeTimeEventSlotRequest() throws IOException {
        String json = """
                {
                     "title": "Title",
                     "location": "New location",
                     "description": "New description",
                     "startTime": "2024-12-25T11:00:00",
                     "endTime": "2024-12-25T13:00:00",
                     "startTimeZoneId": "America/New_York",
                     "endTimeZoneId": "America/New_York"
                }
                """;

        TimeEventSlotRequest expected = TimeEventSlotRequest.builder()
                .title("Title")
                .location("New location")
                .description("New description")
                .startTime(LocalDateTime.parse("2024-12-25T11:00:00"))
                .endTime(LocalDateTime.parse("2024-12-25T13:00:00"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("America/New_York"))
                .build();

        TimeEventSlotRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);

    }
}
