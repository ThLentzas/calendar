package org.example.calendar.event.time;

import org.example.calendar.entity.TimeEventSlot;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.time.dto.TimeEventInvitationRequest;
import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.event.slot.time.TimeEventSlotService;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.example.calendar.email.EmailService;
import org.example.calendar.entity.TimeEvent;
import org.example.calendar.entity.User;
import org.example.calendar.event.time.projection.TimeEventProjection;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.UserRepository;
import org.example.calendar.utils.EventUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeEventService {
    private final TimeEventRepository timeEventRepository;
    private final UserRepository userRepository;
    private final TimeEventSlotService timeEventSlotService;
    private final EmailService emailService;
    private static final String EVENT_NOT_FOUND_MSG = "Time event not found with id: ";

    @Transactional
    public UUID createEvent(Long userId, TimeEventRequest eventRequest) {
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
                .recurrenceFrequency(eventRequest.getRecurrenceFrequency())
                .recurrenceStep(eventRequest.getRecurrenceStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(eventRequest.getMonthlyRecurrenceType())
                .recurrenceDuration(eventRequest.getRecurrenceDuration())
                .recurrenceEndDate(eventRequest.getRecurrenceEndDate())
                .numberOfOccurrences(eventRequest.getNumberOfOccurrences())
                .organizerId(user.getId())
                .build();
        /*
            For events that are set to recur forever, we choose an arbitrary limit of 100 years and set the recurrence
            end date to 100 years in the future. This effectively treats the event as an UNTIL_DATE event, with the
            recurrence end date now being 100 years from the start date. This is one approach we use to handle the
            "forever" recurrence scenario.
        */
        if (event.getRecurrenceFrequency() != RecurrenceFrequency.NEVER && event.getRecurrenceDuration() == RecurrenceDuration.FOREVER) {
            event.setRecurrenceEndDate(LocalDate.from(event.getStartTime().plusYears(100)));
        }

        this.timeEventRepository.create(event);
        eventRequest.setGuestEmails(EventUtils.processGuestEmails(user, eventRequest.getGuestEmails()));
        this.timeEventSlotService.create(eventRequest, event);
        TimeEventInvitationRequest emailRequest = TimeEventInvitationRequest.builder()
                .eventName(eventRequest.getTitle())
                .location(eventRequest.getLocation())
                .organizer(user.getUsername())
                .guestEmails(eventRequest.getGuestEmails())
                .recurrenceFrequency(eventRequest.getRecurrenceFrequency())
                .recurrenceStep(eventRequest.getRecurrenceStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(eventRequest.getMonthlyRecurrenceType())
                .recurrenceDuration(eventRequest.getRecurrenceDuration())
                .recurrenceEndDate(eventRequest.getRecurrenceEndDate())
                .numbersOfOccurrences(eventRequest.getNumberOfOccurrences())
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .startTimeZoneId(eventRequest.getStartTimeZoneId())
                .endTimeZoneId(eventRequest.getEndTimeZoneId())
                .build();
        this.emailService.sendInvitationEmail(emailRequest);

        return event.getId();
    }

    // Either the event does not exist, or the user that made the request is not organizer of the event. Both lead to 404.
    @Transactional
    public void updateEvent(Long userId, UUID eventId, TimeEventRequest eventRequest) {
        TimeEventProjection projection = this.timeEventRepository.findByEventAndUserId(eventId, userId).orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId));
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        TimeEvent original = TimeEvent.builder()
                .id(projection.getId())
                .startTime(projection.getStarTime())
                .startTimeZoneId(projection.getStartTimeZoneId())
                .endTime(projection.getEndTime())
                .endTimeZoneId(projection.getEndTimeZoneId())
                .recurrenceFrequency(projection.getRecurrenceFrequency())
                .recurrenceStep(projection.getRecurrenceStep())
                .weeklyRecurrenceDays(projection.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(projection.getMonthlyRecurrenceType())
                .recurrenceDuration(projection.getRecurrenceDuration())
                .recurrenceEndDate(projection.getRecurrenceEndDate())
                .numberOfOccurrences(projection.getNumberOfOccurrences())
                .build();

        // TimeEvent modified = original; Shallow copy it would also change the original
        TimeEvent modified = new TimeEvent(original);
        eventRequest.setGuestEmails(EventUtils.processGuestEmails(user, eventRequest.getGuestEmails()));
        EventUtils.setFrequencyProperties(eventRequest, modified);
        modified.setStartTime(eventRequest.getStartTime());
        modified.setStartTimeZoneId(eventRequest.getStartTimeZoneId());
        modified.setEndTime(eventRequest.getEndTime());
        modified.setEndTimeZoneId(eventRequest.getEndTimeZoneId());
        if (!EventUtils.hasSameFrequencyProperties(original, modified)
                || !original.getStartTime().isEqual(modified.getStartTime())
                || !original.getStartTimeZoneId().equals(modified.getStartTimeZoneId())
                || !original.getEndTime().isEqual(modified.getEndTime())
                || !original.getEndTimeZoneId().equals(modified.getEndTimeZoneId())) {
            this.timeEventSlotService.deleteEventSlotsByEventId(original.getId());
            this.timeEventSlotService.create(eventRequest, modified);
            this.timeEventRepository.update(original, modified);
        } else {
            List<TimeEventSlot> eventSlots = projection.getEventSlots().stream()
                    .map(slotProjection -> TimeEventSlot.builder()
                            .id(slotProjection.getId())
                            .title(slotProjection.getTitle())
                            .location(slotProjection.getLocation())
                            .description(slotProjection.getDescription())
                            .guestEmails(slotProjection.getGuestEmails())
                            .build())
                    .collect(Collectors.toList());
            this.timeEventSlotService.updateEventSlotsForEvent(eventRequest, eventSlots);
        }
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
    public List<TimeEventSlotPublicProjection> findEventSlotsByEventId(UUID eventId, Long userId) {
        return this.timeEventSlotService.findEventSlotsByEventAndUserId(eventId, userId);
    }

    /*
        Calling getReferenceById() will not work like it did before because we need all the day events that the user
        is either the Organizer(id) but also those that they are invited as guest via their email. We need both.
     */
    public List<TimeEventSlotPublicProjection> findEventSlotsByUserInDateRange(Long userId, LocalDateTime startTime, ZoneId startTimeZoneId, LocalDateTime endTime, ZoneId endTimeZoneId) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);

        return this.timeEventSlotService.findEventSlotsByUserInDateRange(user, startTime, startTimeZoneId, endTime, endTimeZoneId);
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
        int deleted = this.timeEventRepository.deleteByEventAndUserId(eventId, userId);
        if (deleted != 1) {
            throw new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + eventId);
        }
    }
}
