package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.calendar.event.day.slot.DayEventSlotService;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

    @Test
    void shouldThrowResourceNotFoundExceptionWhenEventIsNotFoundForFindEventSlotsByEventId() {
        UUID eventId = UUID.randomUUID();

        when(this.dayEventRepository.findByIdFetchingUser(eventId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                        this.underTest.findEventSlotsByEventId(mock(Jwt.class), eventId))
                .withMessage("Day event not found with id: " + eventId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenCurrentUserIsNotHostOfRequestedEvent() {
        UUID eventId = UUID.randomUUID();
        DayEvent dayEvent = createDayEvent(eventId);
        Jwt mockJwt = mock(Jwt.class);
        User user = User.builder()
                .id(2L)
                .build();

        when(this.dayEventRepository.findByIdFetchingUser(eventId)).thenReturn(Optional.of(dayEvent));
        when(mockJwt.getSubject()).thenReturn(String.valueOf(2L));
        when(this.userRepository.getReferenceById(user.getId())).thenReturn(user);

        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() ->
                        this.underTest.findEventSlotsByEventId(mockJwt, eventId))
                .withMessage("Access Denied");
    }

    private DayEvent createDayEvent(UUID eventId) {
        User user = User.builder()
                .id(1L)
                .build();
        DayEvent dayEvent = new DayEvent();
        dayEvent.setId(eventId);
        dayEvent.setStartDate(LocalDate.parse("2024-10-11"));
        dayEvent.setEndDate(LocalDate.parse("2024-10-15"));
        dayEvent.setRepetitionFrequency(RepetitionFrequency.NEVER);
        dayEvent.setUser(user);

        return dayEvent;
    }
}
