package org.example.google_calendar_clone.calendar.event.slot.day.dto;

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

// https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-testing.html @JsonTest
@JsonTest
class DayEventSlotDTOJsonTest {
    @Autowired
    private JacksonTester<DayEventSlotDTO> jacksonTester;
    @Autowired
    private JacksonTester<List<DayEventSlotDTO>> listJacksonTester;

    @Test
    void shouldSerializeDayEventSlot() throws IOException {
        DayEventSlotDTO expected = DayEventSlotDTO.builder()
                .id(UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d"))
                .title("Title")
                .startDate(LocalDate.parse("2024-09-29"))
                .endDate(LocalDate.parse("2024-12-20"))
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .dayEventId(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"))
                .build();

        String json = """
                {
                    "id": "eede21d1-c2f1-4dc8-9913-a173c491f07d",
                    "title": "Title",
                    "location": "Location",
                    "description": "Description",
                    "organizer": "ellyn.roberts",
                    "guestEmails": [],
                    "startDate": "2024-09-29",
                    "endDate": "2024-12-20",
                    "dayEventId": "9c6f34b8-4128-42ec-beb1-99c35af8d7fa"
                }
                """;

        JsonContent<DayEventSlotDTO> actual = this.jacksonTester.write(expected);

        assertThat(actual).isEqualToJson(json);
    }

    @Test
    void shouldSerializeDayEventSlotList() throws IOException {
        DayEventSlotDTO dayEventSlotDTO1 = DayEventSlotDTO.builder()
                .id(UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d"))
                .title("Event title")
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
                .title("Event title")
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
                        "title": "Event title",
                        "location": "Location",
                        "description": "Description",
                        "organizer": "ellyn.roberts",
                        "guestEmails": [],
                        "startDate": "2024-10-11",
                        "endDate": "2024-10-15",
                        "dayEventId": "9c6f34b8-4128-42ec-beb1-99c35af8d7fa"
                    },
                    {
                        "id": "0ad2f73a-a0bb-406f-9803-4db554beb345",
                        "title": "Event title",
                        "location": "Location",
                        "description": "Description",
                        "organizer": "ellyn.roberts",
                        "guestEmails": [],
                        "startDate": "2025-01-10",
                        "endDate": "2025-01-14",
                        "dayEventId": "9c6f34b8-4128-42ec-beb1-99c35af8d7fa"
                    }
                ]
                """;

        JsonContent<List<DayEventSlotDTO>> actual = this.listJacksonTester.write(eventSlots);

        assertThat(actual).isEqualToJson(json);
    }
}
