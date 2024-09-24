package org.example.calendar.event.day;

import org.example.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.slot.day.DayEventSlotService;
import org.example.calendar.event.slot.day.dto.DayEventSlotDTO;
import org.example.calendar.email.EmailService;
import org.example.calendar.entity.DayEvent;
import org.example.calendar.entity.User;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventService {
    private final DayEventSlotService dayEventSlotService;
    private final DayEventRepository dayEventRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final String EVENT_NOT_FOUND_MSG = "Day event not found with id: ";

    @Transactional
    public UUID create(Long userId, DayEventRequest eventRequest) {
        /*
            The current authenticated user is the organizer of the event. We can't call getReferenceById(), we need the
            username of the user to set it as the organizer in the invitation email template
         */
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        DayEvent event = DayEvent.builder()
                .startDate(eventRequest.getStartDate())
                .endDate(eventRequest.getEndDate())
                .recurrenceFrequency(eventRequest.getRecurrenceFrequency())
                .recurrenceStep(eventRequest.getRecurrenceStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(eventRequest.getMonthlyRecurrenceType())
                .recurrenceDuration(eventRequest.getRecurrenceDuration())
                .recurrenceEndDate(eventRequest.getRecurrenceEndDate())
                .numberOfOccurrences(eventRequest.getNumberOfOccurrences())
                .user(user)
                .build();

        /*
            For events that are set to recur forever, we choose an arbitrary limit of 100 years and set the recurrence
            end date to 100 years in the future. This effectively treats the event as an UNTIL_DATE event, with the
            recurrence end date now being 100 years from the start date. This is one approach we use to handle the
            "forever" recurrence scenario.
        */
        if (eventRequest.getRecurrenceFrequency() != RecurrenceFrequency.NEVER && eventRequest.getRecurrenceDuration() == RecurrenceDuration.FOREVER) {
            event.setRecurrenceEndDate(eventRequest.getStartDate().plusYears(100));
        }

        this.dayEventRepository.save(event);
        this.dayEventSlotService.create(eventRequest, event);
        DayEventInvitationRequest emailRequest = DayEventInvitationRequest.builder()
                .eventName(eventRequest.getTitle())
                .organizer(user.getUsername())
                .location(eventRequest.getLocation())
                .description(eventRequest.getDescription())
                .guestEmails(eventRequest.getGuestEmails())
                .recurrenceFrequency(event.getRecurrenceFrequency())
                .recurrenceStep(event.getRecurrenceStep())
                .weeklyRecurrenceDays(event.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(event.getMonthlyRecurrenceType())
                .recurrenceDuration(event.getRecurrenceDuration())
                .recurrenceEndDate(event.getRecurrenceEndDate())
                .numbersOfOccurrences(event.getNumberOfOccurrences())
                .startDate(event.getStartDate())
                .build();
        this.emailService.sendInvitationEmail(emailRequest);

        return event.getId();
    }

    // Either the event does not exist, or the user that made the request is not organizer of the event. Both lead to 404.
    @Transactional
    public void update(Long userId, UUID eventId, DayEventRequest eventRequest) {
        DayEvent event = this.dayEventRepository.findByEventAndUserId(eventId, userId).orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));

        this.dayEventSlotService.updateEventSlotsForEvent(eventRequest, event);
        this.dayEventRepository.save(event);
    }

    /*
        There are 2 cases where the findEventSlotsByEventId() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.

        Previous approach that was improved:
            if (!this.dayEventRepository.existsByEventIdAndUserId(eventId, userId)) {
                throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
            }
        With the above approach we check if the user that made the request is organizer at the event. We optimize the
        query to do the look-up like this:
            WHERE des.id = :slotId AND de.user.id = :userId
        Both cases that are mentioned above are covered by 1 query.

        If the event is not found or the user is not organizer of the event an empty list will be returned.
     */
    public List<DayEventSlotDTO> findEventSlotsByEventId(UUID eventId, Long userId) {
        return this.dayEventSlotService.findEventSlotsByEventId(eventId, userId);
    }

    /*
        Calling getReferenceById() will not work like it did before because we need all the day events that the user
        is either the Organizer(id) but also those that they are invited as guest via their email. We need both.
     */
    public List<DayEventSlotDTO> findEventSlotsByUserInDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        return this.dayEventSlotService.findEventSlotsByUserInDateRange(user, startDate, endDate);
    }

    /*
        There are 2 cases where the deleteById() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.

        Previous approach that was improved:
            if (!this.dayEventRepository.existsByEventIdAndUserId(eventId, userId)) {
                throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
            }
        With the above approach we check if the user that made the request is organizer at the event. We optimize the
        query in the deleteByEventAndUserId to do the look-up like this:
            WHERE des.id = :eventId AND de.user.id = :userId
        Both cases that are mentioned above are covered by 1 query.
     */
    @Transactional
    public void deleteEventById(UUID eventId, Long userId) {
        int deleted = this.dayEventRepository.deleteByEventAndUserId(eventId, userId);
        if (deleted != 1) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
        }
    }
}
