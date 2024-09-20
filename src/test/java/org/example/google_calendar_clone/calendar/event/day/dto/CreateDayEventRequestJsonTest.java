package org.example.google_calendar_clone.calendar.event.day.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CreateDayEventRequestJsonTest {
    @Autowired
    private JacksonTester<CreateDayEventRequest> jacksonTester;

    @Test
    void shouldDeserializeCreateDayEventRequest() throws IOException {
        String json = """
                {
                     "title": "Event title",
                     "location": "Location",
                     "description": "Description",
                     "startDate": "2024-10-11",
                     "endDate": "2024-10-15",
                     "repetitionFrequency": "WEEKLY",
                     "repetitionStep": 3,
                     "weeklyRecurrenceDays": ["MONDAY", "FRIDAY"],
                     "repetitionDuration": "N_REPETITIONS",
                     "repetitionOccurrences": 3
                }
                """;

        CreateDayEventRequest expected = CreateDayEventRequest.builder()
                .title("Event title")
                .location("Location")
                .description("Description")
                .startDate(LocalDate.parse("2024-10-11"))
                .endDate(LocalDate.parse("2024-10-15"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(3)
                .build();

        CreateDayEventRequest actual = this.jacksonTester.parseObject(json);

        /*
            DayEventRequest has @EqualsAndHashCode(callSuperTrue) and the parent class is annotated with
            @EqualsAndHashCode which will include all the properties. We could also use:

            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

            The assertion is being done field by field.
         */
        assertThat(actual).isEqualTo(expected);
    }
}

