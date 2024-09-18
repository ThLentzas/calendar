package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventInvitationRequest;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.TimeEventSlotService;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.email.EmailService;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeEventService implements IEventService<TimeEventRequest, TimeEventSlotDTO> {
    private final UserRepository userRepository;
    private final TimeEventRepository timeEventRepository;
    private final TimeEventSlotService timeEventSlotService;
    private final EmailService emailService;
    private static final String EVENT_NOT_FOUND_MSG = "Time event not found with id: ";

    @Override
    public UUID create(Long userId, TimeEventRequest eventRequest) {
        /*
            The current authenticated user is the organizer of the event. We can't call getReferenceById(), we need the
            username of the user to set it as the organizer in the invitation email template
         */
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        TimeEvent event = TimeEvent.builder()
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .startTimeZoneId(eventRequest.getStartTimeZoneId())
                .endTimeZoneId(eventRequest.getEndTimeZoneId())
                .repetitionFrequency(eventRequest.getRepetitionFrequency())
                .repetitionStep(eventRequest.getRepetitionStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRepetitionType(eventRequest.getMonthlyRepetitionType())
                .repetitionDuration(eventRequest.getRepetitionDuration())
                .repetitionEndDate(eventRequest.getRepetitionEndDate())
                .repetitionOccurrences(eventRequest.getRepetitionOccurrences())
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
        TimeEventInvitationRequest emailRequest = TimeEventInvitationRequest.builder()
                .eventName(eventRequest.getName())
                .location(eventRequest.getLocation())
                .organizer(user.getUsername())
                .guestEmails(eventRequest.getGuestEmails())
                .repetitionFrequency(event.getRepetitionFrequency())
                .repetitionStep(event.getRepetitionStep())
                .weeklyRecurrenceDays(event.getWeeklyRecurrenceDays())
                .monthlyRepetitionType(event.getMonthlyRepetitionType())
                .repetitionDuration(event.getRepetitionDuration())
                .repetitionEndDate(event.getRepetitionEndDate())
                .repetitionOccurrences(event.getRepetitionOccurrences())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .startTimeZoneId(event.getStartTimeZoneId())
                .endTimeZoneId(event.getEndTimeZoneId())
                .build();
        this.emailService.sendInvitationEmail(emailRequest);

        return event.getId();
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
    @Override
    public List<TimeEventSlotDTO> findEventSlotsByEventId(Long userId, UUID eventId) {
        if (!this.timeEventRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
        }

        return this.timeEventSlotService.findEventSlotsByEventId(eventId);
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
    @Override
    public void deleteById(Long userId, UUID eventId) {
        if (!this.timeEventRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
        }

        this.timeEventRepository.deleteById(eventId);
    }

    public List<TimeEventSlotDTO> findEventSlotsByUserInDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        return this.timeEventSlotService.findEventSlotsByUserInDateRange(user, startTime, endTime);
    }
}
