package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.DayEventSlotService;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.email.EmailService;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventService implements IEventService<DayEventRequest, DayEventSlotDTO> {
    private final DayEventSlotService dayEventSlotService;
    private final DayEventRepository dayEventRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final String EVENT_NOT_FOUND_MSG = "Day event not found with id: ";

    @Override
    public UUID create(Long userId, DayEventRequest eventRequest) {
        /*
            The current authenticated user is the organizer of the event. We can't call getReferenceById(), we need the
            username of the user to set it as the organizer in the invitation email template
         */
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        DayEvent event = DayEvent.builder()
                .startDate(eventRequest.getStartDate())
                .endDate(eventRequest.getEndDate())
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
        if (eventRequest.getRepetitionFrequency() != RepetitionFrequency.NEVER
                && eventRequest.getRepetitionDuration() == RepetitionDuration.FOREVER) {
            event.setRepetitionEndDate(eventRequest.getStartDate().plusYears(100));
        }

        this.dayEventRepository.save(event);
        this.dayEventSlotService.create(eventRequest, event);
        DayEventInvitationRequest emailRequest = DayEventInvitationRequest.builder()
                .eventName(eventRequest.getName())
                .organizer(user.getUsername())
                .location(eventRequest.getLocation())
                .description(eventRequest.getDescription())
                .guestEmails(eventRequest.getGuestEmails())
                .repetitionFrequency(event.getRepetitionFrequency())
                .repetitionStep(event.getRepetitionStep())
                .weeklyRecurrenceDays(event.getWeeklyRecurrenceDays())
                .monthlyRepetitionType(event.getMonthlyRepetitionType())
                .repetitionDuration(event.getRepetitionDuration())
                .repetitionEndDate(event.getRepetitionEndDate())
                .repetitionOccurrences(event.getRepetitionOccurrences())
                .startDate(event.getStartDate())
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
    public List<DayEventSlotDTO> findEventSlotsByEventId(Long userId, UUID eventId) {
        if (!this.dayEventRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
        }

        return this.dayEventSlotService.findEventSlotsByEventId(eventId);
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
        if(!this.dayEventRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
        }

        this.dayEventRepository.deleteById(eventId);
    }

    /*
        Calling getReferenceById() will not work like it did before because we need all the day events that the user
        is either the Organizer(id) but also those that they are invited as guest via their email. We need both.
     */
    public List<DayEventSlotDTO> findEventSlotsByUserInDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        return this.dayEventSlotService.findEventSlotsByUserInDateRange(user, startDate, endDate);
    }
}
