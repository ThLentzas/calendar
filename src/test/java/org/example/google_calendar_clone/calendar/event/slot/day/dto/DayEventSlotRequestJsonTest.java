package org.example.google_calendar_clone.calendar.event.slot.day.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class DayEventSlotRequestJsonTest {
    @Autowired
    private JacksonTester<DayEventSlotRequest> jacksonTester;

    @Test
    void shouldDeserializeDayEventSlotRequest() throws IOException {
        String json = """
                {
                     "title": "Title",
                     "location": "New location",
                     "description": "New description",
                     "startDate": "2024-12-25",
                     "endDate": "2024-12-27"
                }
                """;

        DayEventSlotRequest expected = DayEventSlotRequest.builder()
                .title("Title")
                .location("New location")
                .description("New description")
                .startDate(LocalDate.parse("2024-12-25"))
                .endDate(LocalDate.parse("2024-12-27"))
                .build();

        DayEventSlotRequest actual = this.jacksonTester.parseObject(json);

        assertThat(actual).isEqualTo(expected);
    }
}
