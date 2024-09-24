package org.example.calendar.event.time;

import org.example.calendar.event.time.TimeEventRepository;
import org.example.calendar.event.time.TimeEventService;
import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.event.slot.time.TimeEventSlotService;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.UserRepository;
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
class TimeEventServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private TimeEventRepository timeEventRepository;
    @Mock
    private TimeEventSlotService timeEventSlotService;
    @InjectMocks
    private TimeEventService underTest;

    /*
        There are 2 cases where the findByEventIdAndUserId() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
         In either case it is 404
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForUpdateEvent() {
        TimeEventRequest eventRequest = TimeEventRequest.builder()
                .title("Event title")
                .build();
        UUID eventId = UUID.randomUUID();

        when(this.timeEventRepository.findByEventIdAndUserId(eventId, 2L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.update(2L, eventId, eventRequest)).withMessage("Time event not found with id: " + eventId);
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

        when(this.timeEventRepository.deleteByEventAndUserId(eventId, 2L)).thenReturn(0);

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.deleteEventById(eventId, 2L)).withMessage("Time event not found with id: " + eventId);
    }
}
