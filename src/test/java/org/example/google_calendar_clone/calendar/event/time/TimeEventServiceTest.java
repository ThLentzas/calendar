package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.slot.TimeEventSlotService;
import org.example.google_calendar_clone.entity.TimeEvent;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldThrowResourceNotFoundExceptionWhenEventIsNotFoundForFindEventSlotsByEventId() {
        UUID eventId = UUID.randomUUID();

        when(this.timeEventRepository.findByIdFetchingUser(eventId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                        this.underTest.findEventSlotsByEventId(mock(Jwt.class), eventId))
                .withMessage("Time event not found with id: " + eventId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenCurrentUserIsNotHostOfRequestedEvent() {
        UUID eventId = UUID.randomUUID();
        TimeEvent timeEvent = createTimeEvent(eventId);
        Jwt mockJwt = mock(Jwt.class);
        User user = User.builder()
                .id(2L)
                .build();

        when(this.timeEventRepository.findByIdFetchingUser(eventId)).thenReturn(Optional.of(timeEvent));
        when(mockJwt.getSubject()).thenReturn(String.valueOf(2L));
        when(this.userRepository.getReferenceById(user.getId())).thenReturn(user);

        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() ->
                        this.underTest.findEventSlotsByEventId(mockJwt, eventId))
                .withMessage("Access Denied");
    }

    private TimeEvent createTimeEvent(UUID eventId) {
        User user = User.builder()
                .id(1L)
                .build();
        TimeEvent timeEvent = new TimeEvent();
        timeEvent.setId(eventId);
        timeEvent.setStartTime(LocalDateTime.now(ZoneId.of("Europe/London")));
        timeEvent.setStartTimeZoneId(ZoneId.of("Europe/London"));
        timeEvent.setEndTime(LocalDateTime.now(ZoneId.of("Europe/London")).plusHours(3));
        timeEvent.setEndTimeZoneId(ZoneId.of("Europe/London"));
        timeEvent.setRepetitionFrequency(RepetitionFrequency.NEVER);
        timeEvent.setUser(user);

        return timeEvent;
    }

}
