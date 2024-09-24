package org.example.calendar.event.slot.day.dto;

import org.example.calendar.entity.DayEvent;
import org.example.calendar.entity.DayEventSlot;
import org.example.calendar.entity.User;
import org.example.calendar.event.slot.day.dto.DayEventSlotDTO;
import org.example.calendar.event.slot.day.dto.DayEventSlotDTOConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DayEventSlotDTOConverterTest {
    private final DayEventSlotDTOConverter underTest = new DayEventSlotDTOConverter();

    @Test
    void shouldConvertDayEventSlotToDTO() {
        User user = new User();
        user.setUsername("Organizer");
        DayEvent dayEvent = new DayEvent();
        dayEvent.setId(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"));
        dayEvent.setUser(user);

        DayEventSlot dayEventSlot = new DayEventSlot();
        dayEventSlot.setId(UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d"));
        dayEventSlot.setTitle("Event name");
        dayEventSlot.setStartDate(LocalDate.parse("2024-10-11"));
        dayEventSlot.setEndDate(LocalDate.parse("2024-10-15"));
        dayEventSlot.setLocation("Location");
        dayEventSlot.setDescription("Description");
        dayEventSlot.setDayEvent(dayEvent);
        dayEventSlot.setGuestEmails(Set.of());

        DayEventSlotDTO expected = DayEventSlotDTO.builder()
                .id(UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d"))
                .title("Event name")
                .startDate(LocalDate.parse("2024-10-11"))
                .endDate(LocalDate.parse("2024-10-15"))
                .location("Location")
                .description("Description")
                .organizer("Organizer")
                .guestEmails(Set.of())
                .dayEventId(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"))
                .build();

        DayEventSlotDTO actual = this.underTest.convert(dayEventSlot);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
