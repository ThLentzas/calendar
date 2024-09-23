package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.slot.day.DayEventSlotService;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DayEventServiceTest {
    @Mock
    private DayEventSlotService dayEventSlotService;
    @Mock
    private DayEventRepository dayEventRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private DayEventService underTest;

    /*
        There are 2 cases where the findByEventIdAndUserId() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        In either case it is 404
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForUpdateEvent() {
        DayEventRequest eventRequest = DayEventRequest.builder()
                .title("Event title")
                .build();
        UUID eventId = UUID.randomUUID();

        when(this.dayEventRepository.findByEventAndUserId(eventId, 2L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.update(2L, eventId, eventRequest)).withMessage("Day event not found with id: " + eventId);
    }

    /*
        There are 2 cases where the deleteEventById() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
         In either case it is 404
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForDeleteById() {
        UUID eventId = UUID.randomUUID();

        when(this.dayEventRepository.deleteByEventAndUserId(eventId, 1L)).thenReturn(0);

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.deleteEventById(eventId, 1L)).withMessage("Day event not found with id: " + eventId);
    }
}
