package org.example.google_calendar_clone.calendar.event.day.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class UpdateDayEventRequestJsonTest {
    @Autowired
    private JacksonTester<UpdateDayEventRequest> jacksonTester;

    @Test
    void shouldDeserializeCreateDayEventRequest() throws IOException {
        String json = """
                {
                     "title": "Event title",
                     "location": "Location",
                     "description": "Description"
                }
                """;

        UpdateDayEventRequest expected = UpdateDayEventRequest.builder()
                .title("Event title")
                .location("Location")
                .description("Description")
                .build();

        UpdateDayEventRequest actual = this.jacksonTester.parseObject(json);

        /*
            DayEventRequest has @EqualsAndHashCode(callSuperTrue) and the parent class is annotated with
            @EqualsAndHashCode which will include all the properties. We could also use:

            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

            The assertion is being done field by field.
         */
        assertThat(actual).isEqualTo(expected);
    }
}
