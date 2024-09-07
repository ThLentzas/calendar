package org.example.google_calendar_clone.calendar.event.day.slot.dto;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class DayEventSlotDTOJsonTest {
    @Autowired
    private JacksonTester<List<DayEventSlotDTO>> jacksonTesterList;

    @Test
    void shouldSerializeDayEventSlotList() throws IOException {
        DayEventSlotDTO dayEventSlotDTO1 = DayEventSlotDTO.builder()
                .id(UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d"))
                .name("Event name")
                .startDate(LocalDate.parse("2024-10-11"))
                .endDate(LocalDate.parse("2024-10-15"))
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .dayEventId(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"))
                .build();

        DayEventSlotDTO dayEventSlotDTO2 = DayEventSlotDTO.builder()
                .id(UUID.fromString("0ad2f73a-a0bb-406f-9803-4db554beb345"))
                .name("Event name")
                .startDate(LocalDate.parse("2025-01-10"))
                .endDate(LocalDate.parse("2025-01-14"))
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .dayEventId(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"))
                .build();

        List<DayEventSlotDTO> eventSlots = List.of(dayEventSlotDTO1, dayEventSlotDTO2);

        String json = """
                [
                    {
                        "id": "eede21d1-c2f1-4dc8-9913-a173c491f07d",
                        "name": "Event name",
                        "location": "Location",
                        "description": "Description",
                        "organizer": "ellyn.roberts",
                        "guestEmails": [],
                        "startDate": "2024-10-11",
                        "endDate": "2024-10-15"
                    },
                    {
                        "id": "0ad2f73a-a0bb-406f-9803-4db554beb345",
                        "name": "Event name",
                        "location": "Location",
                        "description": "Description",
                        "organizer": "ellyn.roberts",
                        "guestEmails": [],
                        "startDate": "2025-01-10",
                        "endDate": "2025-01-14"
                    }
                ]
                """;

        JsonContent<List<DayEventSlotDTO>> actual = this.jacksonTesterList.write(eventSlots);

        assertThat(actual).isEqualToJson(json);
    }
}
