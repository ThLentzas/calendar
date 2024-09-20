package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.calendar.event.day.dto.UpdateDayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.DayEventSlotService;
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
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForUpdateEvent() {
        UpdateDayEventRequest eventRequest = UpdateDayEventRequest.builder()
                .title("Event title")
                .build();
        UUID eventId = UUID.randomUUID();

        when(this.dayEventRepository.findByEventIdAndUserId(eventId, 2L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                        this.underTest.update(2L, eventId, eventRequest))
                .withMessage("Day event not found with id: " + eventId);
    }

    /*
        There are 2 cases where the findEventSlotsByEventId() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForFindEventSlotsByEventId() {
        UUID eventId = UUID.randomUUID();

        when(this.dayEventRepository.existsByEventIdAndUserId(eventId, 2L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                        this.underTest.findEventSlotsByEventId(2L, eventId))
                .withMessage("Day event not found with id: " + eventId);
    }

    /*
        There are 2 cases where the deleteById() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForDeleteById() {
        UUID eventId = UUID.randomUUID();

        when(this.dayEventRepository.existsByEventIdAndUserId(eventId, 2L)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                        this.underTest.deleteById(2L, eventId))
                .withMessage("Day event not found with id: " + eventId);
    }
}
