package org.example.google_calendar_clone.calendar.event.day.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class DayEventRequestJsonTest {
    @Autowired
    private JacksonTester<DayEventRequest> jacksonTester;

    @Test
    void shouldDeserializeDayEventRequest() throws IOException {
        String json = """
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
                     "repetitionCount": "3"
                }
                """;

        DayEventRequest expected = DayEventRequest.builder()
                .name("Event name")
                .location("Location")
                .description("Description")
                .startDate(LocalDate.parse("2024-10-11"))
                .endDate(LocalDate.parse("2024-10-15"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(3)
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

