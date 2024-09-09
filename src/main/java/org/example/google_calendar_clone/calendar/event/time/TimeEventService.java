package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.TimeEventSlotService;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeEventService implements IEventService<TimeEventRequest> {
    private final UserRepository userRepository;
    private final TimeEventRepository timeEventRepository;
    private final TimeEventSlotService timeEventSlotService;
    private static final Logger logger = LoggerFactory.getLogger(TimeEventService.class);

    @Override
    public UUID create(Jwt jwt, TimeEventRequest eventRequest) {
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        TimeEvent event = TimeEvent.builder()
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .startTimeZoneId(eventRequest.getStartTimeZoneId())
                .endTimeZoneId(eventRequest.getEndTimeZoneId())
                .repetitionFrequency(eventRequest.getRepetitionFrequency())
                .repetitionStep(eventRequest.getRepetitionStep())
                .monthlyRepetitionType(eventRequest.getMonthlyRepetitionType())
                .repetitionDuration(eventRequest.getRepetitionDuration())
                .repetitionEndDate(eventRequest.getRepetitionEndDate())
                .repetitionCount(eventRequest.getRepetitionCount())
                .user(user)
                .build();
        /*
            For events that are set to be repeated for FOREVER we choose an arbitrary number like 100 years
            and set the repetition End Date to plus 100 years. We treat the event then as UNTIL_DATE but
            now the repetitionEndDate will be 100 years from now. This is 1 way to approach the FOREVER
            case
        */
        if (event.getRepetitionFrequency() != RepetitionFrequency.NEVER
                && event.getRepetitionDuration() == RepetitionDuration.FOREVER) {
            event.setRepetitionEndDate(LocalDate.from(event.getStartTime().plusYears(100)));
        }

        this.timeEventRepository.save(event);
        this.timeEventSlotService.create(eventRequest, event);

        return event.getId();
    }

    @Transactional
    public List<TimeEventSlotDTO> findEventSlotsByEventId(Jwt jwt, UUID id) {
        TimeEvent event = this.timeEventRepository.findByIdFetchingUser(id).orElseThrow(() ->
                new ResourceNotFoundException("Time event not found with id: " + id));
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        if (!isOrganizerOfEvent(user, event)) {
            logger.info("User with id: {} is not host of event with id: {}", user.getId(), id);
            throw new AccessDeniedException("Access Denied");
        }

        return this.timeEventSlotService.findEventSlotsByEventId(id);
    }

    private boolean isOrganizerOfEvent(User user, TimeEvent event) {
        return user.equals(event.getUser());
    }
}
