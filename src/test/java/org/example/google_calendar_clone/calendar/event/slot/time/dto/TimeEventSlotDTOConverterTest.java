package org.example.google_calendar_clone.calendar.event.slot.time.dto;

import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEventSlotDTOConverterTest {
    private final TimeEventSlotDTOConverter underTest = new TimeEventSlotDTOConverter();

    @Test
    void shouldConvertTimeEventSlotToTimeEventSlotDTO() {
        User user = new User();
        user.setUsername("ellyn.roberts");
        TimeEvent event = new TimeEvent();
        event.setId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"));
        event.setUser(user);

        TimeEventSlot eventSlot = new TimeEventSlot();
        eventSlot.setId(UUID.fromString("e431687e-7251-4ac6-b797-c107064af135"));
        eventSlot.setTitle("Event Title");
        eventSlot.setStartTime(LocalDateTime.parse("2024-10-11T09:00:00"));
        eventSlot.setEndTime(LocalDateTime.parse("2024-10-15T14:00:00"));
        eventSlot.setStartTimeZoneId(ZoneId.of("Europe/London"));
        eventSlot.setEndTimeZoneId(ZoneId.of("Europe/London"));
        eventSlot.setLocation("Location");
        eventSlot.setDescription("Description");
        eventSlot.setGuestEmails(Set.of());
        eventSlot.setTimeEvent(event);

        TimeEventSlotDTO expected = TimeEventSlotDTO.builder()
                .id(UUID.fromString("e431687e-7251-4ac6-b797-c107064af135"))
                .title("Event Title")
                .startTime(LocalDateTime.parse("2024-10-11T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .timeEventId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"))
                .build();

        TimeEventSlotDTO actual = this.underTest.convert(eventSlot);

        assertThat(actual).isEqualTo(expected);
    }
}
