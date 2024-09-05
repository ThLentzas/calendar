package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.DayEventSlotService;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventService implements IEventService<DayEventRequest, DayEvent> {
    private final DayEventSlotService dayEventSlotService;
    private final DayEventRepository dayEventRepository;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(DayEventService.class);

    //toDo: add invite link in the dto
    @Override
    public DayEvent create(Jwt jwt, DayEventRequest dayEventRequest) {
        // The current authenticated user is the host of the event
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        DayEvent dayEvent = new DayEvent();
        /*
            For events that are set to be repeated for FOREVER we choose an arbitrary number like 100 years
            and set the repetition End Date to plus 100 years. We treat the event then as UNTIL_DATE but
            now the repetitionEndDate will be 100 years from now. This is 1 way to approach the FOREVER
            case
        */
        if (dayEventRequest.getRepetitionFrequency() != RepetitionFrequency.NEVER
                && dayEventRequest.getRepetitionDuration() == RepetitionDuration.FOREVER) {
            dayEvent.setRepetitionEndDate(dayEvent.getStartDate().plusYears(100));
        }

        dayEvent.setStartDate(dayEventRequest.getStartDate());
        dayEvent.setEndDate(dayEventRequest.getEndDate());
        dayEvent.setRepetitionFrequency(dayEventRequest.getRepetitionFrequency());
        dayEvent.setRepetitionStep(dayEventRequest.getRepetitionStep());
        dayEvent.setMonthlyRepetitionType(dayEventRequest.getMonthlyRepetitionType());
        dayEvent.setRepetitionDuration(dayEventRequest.getRepetitionDuration());
        dayEvent.setRepetitionEndDate(dayEventRequest.getRepetitionEndDate());
        dayEvent.setRepetitionCount(dayEventRequest.getRepetitionCount());
        dayEvent.setUser(user);

        this.dayEventRepository.save(dayEvent);
        this.dayEventSlotService.create(dayEventRequest, dayEvent);

        return dayEvent;
    }

    @Transactional
    public List<DayEventSlotDTO> findEventSlotsByEventId(Jwt jwt, UUID id) {
        DayEvent dayEvent = this.dayEventRepository.findByIdFetchingUser(id).orElseThrow(() ->
                new ResourceNotFoundException("Day event not found with id: " + id));
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        if (!isOrganizerOfEvent(user, dayEvent)) {
            logger.info("User with id: {} is not host of event with id: {}", user.getId(), id);
            throw new AccessDeniedException("Access Denied");
        }

        return this.dayEventSlotService.findEventSlotsByEventId(id);
    }

    private boolean isOrganizerOfEvent(User user, DayEvent dayEvent) {
        return user.equals(dayEvent.getUser());
    }
}
