package org.example.calendar.event.day.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class DayEventRequestJsonTest {
    @Autowired
    private JacksonTester<DayEventRequest> jacksonTester;

    @Test
    void shouldDeserializeDayEventRequest() throws IOException {
        String json = """
                {
                     "title": "Event title",
                     "location": "Location",
                     "description": "Description",
                     "startDate": "2024-10-11",
                     "endDate": "2024-10-15",
                     "recurrenceFrequency": "WEEKLY",
                     "recurrenceStep": 3,
                     "weeklyRecurrenceDays": ["MONDAY", "FRIDAY"],
                     "recurrenceDuration": "N_OCCURRENCES",
                     "numberOfOccurrences": 3
                }
                """;

        DayEventRequest expected = DayEventRequest.builder()
                .title("Event title")
                .location("Location")
                .description("Description")
                .startDate(LocalDate.parse("2024-10-11"))
                .endDate(LocalDate.parse("2024-10-15"))
                .recurrenceFrequency(RecurrenceFrequency.WEEKLY)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .recurrenceStep(3)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(3)
                .build();

        DayEventRequest actual = this.jacksonTester.parseObject(json);

        /*
            DayEventRequest has @EqualsAndHashCode(callSuperTrue) and the parent class is annotated with
            @EqualsAndHashCode which will include all the properties. We could also use:

            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

            The assertion is being done field by field.
         */
        assertThat(actual).isEqualTo(expected);
    }
}

