package org.example.calendar.event.slot.day;

import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.event.dto.InviteGuestsRequest;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.slot.day.dto.DayEventSlotRequest;
import org.example.calendar.entity.DayEvent;
import org.example.calendar.entity.DayEventSlot;
import org.example.calendar.entity.User;
import org.example.calendar.event.slot.day.projection.DayEventSlotProjection;
import org.example.calendar.event.slot.projection.EventSlotWithGuestsProjection;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.UserRepository;
import org.example.calendar.utils.DateUtils;
import org.example.calendar.utils.EventUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventSlotService {
    private final DayEventSlotRepository eventSlotRepository;
    private final UserRepository userRepository;
    private static final String EVENT_SLOT_NOT_FOUND_MSG = "Day event slot not found with id: ";

    /*
        For every event, we need to compute the future day events as long as it continues to recur. In the DAILY
        recurrence case, we begin from the event's start date and calculate each event occurrence until the recurrence
        end date (the date when the recurring event should stop). As long as the current start date is before the
        recurrence end date, we create a day event slot where:
            The start date will be the current date being processed.
            The end date will be calculated by adding the duration of the event, which is determined by the interval
            between the initial start and end dates. This duration is computed using
            ChronoUnit.DAYS.between(dayEvent.getStartDate(), dayEvent.getEndDate()).
        We advance the start date by the number of days specified in the recurrence step (e.g., every 2 days, weeks,
        months, etc.) and repeat the process.
    */
    @Transactional
    public void create(DayEventRequest eventRequest, DayEvent event) {
        switch (event.getRecurrenceFrequency()) {
            // Since the event is not recurring, we only create 1 DayEventSlot
            case NEVER -> createDayEventSlot(eventRequest, event, event.getStartDate());
            case DAILY -> {
                createUntilDateDailyEventSlots(eventRequest, event);
                createNOccurrencesDailyEventSlots(eventRequest, event);
            }
            case WEEKLY -> {
                createUntilDateWeeklyEventSlots(eventRequest, event);
                createNOccurrencesWeeklyEventSlots(eventRequest, event);
            }
            case MONTHLY -> {
                if (event.getMonthlyRecurrenceType().equals(MonthlyRecurrenceType.SAME_WEEKDAY)) {
                    createUntilDateMonthlySameWeekdayEventSlots(eventRequest, event);
                    createNOccurrencesMonthlySameWeekdayEventSlots(eventRequest, event);
                } else {
                    // For events that are recurring Monthly, over N Months, Annually, or over N years we will use the
                    // same method to take into consideration things like leap years for events at 29 February and
                    // events that are to occur at the last day of the month
                    createUntilDateSameDayEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                    createNOccurrencesSameDayEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                }
            }
            /*
                For annually recurring events, apart from 29th of February, all we need to know is, that months have
                the same numbers of days in different years.

                January: Always has 31 days,
                February:
                    Non-leap years: February has 28 days.
                    Leap years: February has 29 days.
                March: Always has 31 days.
                April: Always has 30 days.
                May: Always has 31 days.
                June: Always has 30 days.
                July: Always has 31 days.
                August: Always has 31 days.
                September: Always has 30 days.
                October: Always has 31 days.
                November: Always has 30 days.
                December: Always has 31 days.
             */
            case ANNUALLY -> {
                createUntilDateSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
                createNOccurrencesSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
            }
        }
    }

    public void updateEventSlotsForEvent(DayEventRequest eventRequest, List<DayEventSlot> eventSlots) {
        DayEventSlotRequest eventSlotRequest = DayEventSlotRequest.builder()
                .title(eventRequest.getTitle())
                .location(eventRequest.getLocation())
                .description(eventRequest.getDescription())
                .build();

        for (DayEventSlot original : eventSlots) {
            DayEventSlot modified = new DayEventSlot();
            modified.setId(original.getId());
            EventUtils.setCommonEventSlotProperties(eventSlotRequest, modified);
            // Can't be null. Emails are processed and if they were null an empty set is returned.
            modified.setGuestEmails(eventRequest.getGuestEmails());
            this.eventSlotRepository.updateEventSlotForEvent(original, modified);
        }
    }

    /*
        For update validation rules look at the DayEventSlotRequestValidator

        There are 2 cases where the existsByEventIdAndUserId() could throw ResourceNotFoundException.
            1. Event slot exists but the authenticated user is not the organizer
            2. Event slot does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.

        Previous approach that was improved:
            DayEventSlot eventSlot = this.dayEventSlotRepository.findByIdOrThrow(slotId, userId);
            if (!this.dayEventRepository.existsByEventIdAndUserId(eventSlot.getDayEvent().getId(), userId)) {
                throw new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId);
            }
        With the above approach we do the lookup for the eventSlot by id, and then we can check if the user that made
        the request is the organizer of the event. We optimize the query to do the look-up like this:
            WHERE des.id = :slotId AND de.user.id = :userId
        Both cases that are mentioned above are covered by 1 query.
    */
    @Transactional
    public void updateEventSlot(Long userId, UUID slotId, DayEventSlotRequest eventSlotRequest) {
        DayEventSlotProjection eventSlotProjection = this.eventSlotRepository.findBySlotAndUserId(slotId, userId).orElseThrow(() -> new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId));
        DayEventSlot original = DayEventSlot.builder()
                .id(eventSlotProjection.getId())
                .title(eventSlotProjection.getTitle())
                .location(eventSlotProjection.getLocation())
                .description(eventSlotProjection.getDescription())
                .startDate(eventSlotProjection.getStartDate())
                .endDate(eventSlotProjection.getEndDate())
                .guestEmails(eventSlotProjection.getGuestEmails())
                .build();
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        DayEventSlot modified = new DayEventSlot(original);
        modified.setStartDate(eventSlotRequest.getStartDate());
        modified.setEndDate(eventSlotRequest.getEndDate());
        EventUtils.setCommonEventSlotProperties(eventSlotRequest, modified);
        modified.setGuestEmails(EventUtils.processGuestEmails(user, eventSlotRequest.getGuestEmails()));

        this.eventSlotRepository.update(original, modified);
    }

    /*
        There are 2 cases where the existsByEventIdAndUserId() could throw ResourceNotFoundException.
            1. Event slot exists but the authenticated user is not the organizer
            2. Event slot does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.

        Previous approach that was improved:
            DayEventSlot eventSlot = this.dayEventSlotRepository.findByIdOrThrow(slotId, userId);
            if (!this.dayEventRepository.existsByEventIdAndUserId(eventSlot.getDayEvent().getId(), userId)) {
                throw new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId);
            }
        With the above approach we do the lookup for the eventSlot by id, and then we can check if the user that made
        the request is the organizer of the event. We optimize the query to do the look-up like this:
            WHERE des.id = :slotId AND de.user.id = :userId
        Both cases that are mentioned above are covered by 1 query.
     */
    @Transactional
    public void inviteGuests(Long userId, UUID slotId, InviteGuestsRequest inviteGuestsRequest) {
        EventSlotWithGuestsProjection projection = this.eventSlotRepository.findBySlotAndUserIdFetchingGuests(slotId, userId).orElseThrow(() -> new ResourceNotFoundException("Day event slot not found with id: " + slotId));
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        Set<String> guestEmails = EventUtils.processGuestEmails(user, inviteGuestsRequest, projection.getGuestEmails());

        this.eventSlotRepository.inviteGuests(projection.getId(), guestEmails);
    }

    public List<DayEventSlotPublicProjection> findEventSlotsByEventAndUserId(UUID eventId, Long userId) {
        return this.eventSlotRepository.findByEventAndUserId(eventId, userId);
    }

    /*
        Returns an event slot where the user is either the organizer or invited as guest
    */
    public DayEventSlotPublicProjection findEventSlotById(Long userId, UUID slotId) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        return this.eventSlotRepository.findByOrganizerOrGuestEmailAndSlotId(slotId, userId, user.getEmail()).orElseThrow(() -> new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId));
    }

    public List<DayEventSlotPublicProjection> findEventSlotsByUserInDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return this.eventSlotRepository.findByUserInDateRange(user.getId(), user.getEmail(), startDate, endDate);
    }

    @Transactional
    public void deleteEventSlotById(UUID slotId, Long userId) {
        int rowsAffected = this.eventSlotRepository.deleteBySlotAndUserId(slotId, userId);
        if (rowsAffected != 1) {
            throw new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId);
        }
    }

    // This method is used to delete the current event slots before creating new ones based on the new recurrence
    // properties when we update an event. We need to delete the previous ones and create the new
    public void deleteEventSlotsByEventId(UUID eventId) {
        this.eventSlotRepository.deleteEventSlotsByEventId(eventId);
    }

    private void createUntilDateDailyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.UNTIL_DATE && dayEvent.getRecurrenceDuration() != RecurrenceDuration.FOREVER) {
            return;
        }

        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRecurrenceEndDate())) {
            createDayEventSlot(dayEventRequest, dayEvent, date);
            date = date.plusDays(dayEvent.getRecurrenceStep());
        }
    }

    private void createNOccurrencesDailyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.N_OCCURRENCES) {
            return;
        }

        LocalDate startDate = dayEvent.getStartDate();
        for (int i = 0; i < dayEvent.getNumberOfOccurrences(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
           /*
                The starDate is updated to the previous value plus the number of the recurrence step which
                can be 1, 2 etc, meaning the event is to occur every 1,2, days until we reach N_OCCURRENCES
            */
            startDate = startDate.plusDays(dayEvent.getRecurrenceStep());
        }
    }

    /*
        Weekly events can occur on certain days of week. ["MONDAY", "THURSDAY", "SATURDAY"]. In the request,
        one of those 3 days will correspond to the date provided by the user. We have 2 cases to consider:
            Case 1: The next day to occur is after the day that corresponds to the start date. For example,
            if 2024-09-12 is a Thursday and we want the event to occur on Monday, Monday is before Thursday so,
            we need to adjust the date. We need to find the difference between the day of the start date and the day
            to occur in our case: Thursday - Monday (in ordinal enum values) will give us a positive difference
            which means the day of the start date is after the day to occur, so we need to subtract days from the
            startDate (startDate = date.minus(difference)), Thursday - Monday = 3 in ordinal,
            startDate = date.minus(3) would result in 2024-09-09 which is a Monday, but this is in the past relative
            to our event. In this case, we need to find the next Monday, this is why the check is there
            !startDate.isBefore(dayEventRequest.getStartDate()). We are interested in future occurrences relative to
            our eventRequest.getStatDate()

            Case 2: Opposite of 1. We need to add the difference but since the number will be negative will add its
            absolute value

            @Test
            void shouldCreateDayEventSlotsWhenEventIsRecurringEveryNWeeksUntilACertainDate(), DayEventSlotServiceTest
     */
    private void createUntilDateWeeklyEventSlots(DayEventRequest eventRequest, DayEvent dayEvent) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.UNTIL_DATE && dayEvent.getRecurrenceDuration() != RecurrenceDuration.FOREVER) {
            return;
        }

        LocalDate startDate;
        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRecurrenceEndDate())) {
            for (DayOfWeek dayOfWeek : dayEvent.getWeeklyRecurrenceDays()) {
                int differenceInDays = dayEvent.getStartDate().getDayOfWeek().getValue() - dayOfWeek.getValue();
                if (differenceInDays > 0) {
                    startDate = date.minusDays(differenceInDays);
                } else {
                    // Math.abs() would also work, we are using the minus operator, which negates the integer(changes the sing)
                    startDate = date.plusDays(-differenceInDays);
                }

                /*
                    The start date must fall within the recurrence end date. For example, if the current day is
                     Thursday, but the event is set to occur on Mondays, we need to handle the situation where Monday
                     comes before Thursday. In this case, for the first occurrence of the event, we consider the first
                     Monday after the start date, ensuring that the event starts on the next available Monday.
                 */
                if (!startDate.isBefore(dayEvent.getStartDate()) && !startDate.isAfter(dayEvent.getRecurrenceEndDate())) {
                    createDayEventSlot(eventRequest, dayEvent, startDate);
                }
            }
            date = date.plusWeeks(dayEvent.getRecurrenceStep());
        }
    }

    /*
        Similar logic with createUntilDateWeeklyEventSlots()

            @Test
            void shouldCreateDayEventSlotsWhenEventIsRecurringEveryNWeeksForNOccurrences(), DayEventSlotServiceTest
     */
    private void createNOccurrencesWeeklyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.N_OCCURRENCES) {
            return;
        }

        int count = 0;
        LocalDate startDate;
        LocalDate date = dayEvent.getStartDate();
        while (count < dayEvent.getNumberOfOccurrences()) {
            for (DayOfWeek dayOfWeek : dayEvent.getWeeklyRecurrenceDays()) {
                int differenceInDays = dayEvent.getStartDate().getDayOfWeek().getValue() - dayOfWeek.getValue();
                if (differenceInDays > 0) {
                    startDate = date.minusDays(differenceInDays);
                } else {
                    // Math.abs() would also, we are using the minus operator, which negates the integer(changes the sing)
                    startDate = date.plusDays(-differenceInDays);
                }
                if (!startDate.isBefore(dayEvent.getStartDate())) {
                    createDayEventSlot(dayEventRequest, dayEvent, startDate);
                    count++;
                    // During the inner loop the count might be equal or greater than the occurrences
                    if (count == dayEvent.getNumberOfOccurrences()) {
                        return;
                    }
                }
            }
            date = date.plusWeeks(dayEvent.getRecurrenceStep());
        }
    }

    // Monthly events that occur the same weekday until a certain date(2nd Tuesday of the month)
    private void createUntilDateMonthlySameWeekdayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.UNTIL_DATE && dayEvent.getRecurrenceDuration() != RecurrenceDuration.FOREVER) {
            return;
        }

        int occurrences = DateUtils.findDayOfMonthOccurrence(dayEvent.getStartDate());
        LocalDate startDate;
        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRecurrenceEndDate())) {
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(YearMonth.of(date.getYear(), date.getMonth()), dayEvent.getStartDate().getDayOfWeek(), occurrences);
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            date = date.plusMonths(dayEvent.getRecurrenceStep());
        }
    }

    // Monthly events that occur the same weekday until a number of occurrences(2nd Tuesday of the month)
    private void createNOccurrencesMonthlySameWeekdayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.N_OCCURRENCES) {
            return;
        }
        int occurrences = DateUtils.findDayOfMonthOccurrence(dayEvent.getStartDate());
        LocalDate startDate = dayEvent.getStartDate();
        for (int i = 0; i < dayEvent.getNumberOfOccurrences(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            startDate = startDate.plusMonths(dayEvent.getRecurrenceStep());
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(YearMonth.of(startDate.getYear(), startDate.getMonth()), dayEvent.getStartDate().getDayOfWeek(), occurrences);
        }
    }

    /*
        When it comes to monthly recurring events on the same day we have to consider an edge case where we want the
        event to occur at the last day of each month for 14 months. If we have an event for the 31 of January
        we can't move to the 31st of February, it is not a valid date. We need to move to the last day of each month.
        Java takes care of the advancing part, where if we are at 31st of January and, we say date = date.plusMonths(1)
        it will take us to 28 or 29 of February depending on if it is a leap year or not. The problem is that when we
        advance again we will move to the 28 or 29 of March which is not the last day of the month. We have to address
        that scenario.
        Logic also explained to the DateUtils.adjustDateForMonth()
     */
    private void createUntilDateSameDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent, ChronoUnit unit) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.UNTIL_DATE && dayEvent.getRecurrenceDuration() != RecurrenceDuration.FOREVER) {
            return;
        }

        /*
            int dayOfMonth = dayEvent.getStartDate().getDayOfMonth();
            for (LocalDate date = dayEvent.getStartDate(); !date.isAfter(dayEvent.getRecurrenceEndDate());
                 date = date.plusMonths(dayEvent.getRecurrenceStep())) {
                date = DateUtils.adjustDateForMonth(dayOfMonth, date);
                createDayEventSlot(dayEventRequest, dayEvent, date);
            }

            The above code would work but the linter would complain as : Refactor the code in order to not assign to this
            loop counter from within the loop body. We increment the months of the date and also adjust the date of
            the month.
         */
        int dayOfMonth = dayEvent.getStartDate().getDayOfMonth();
        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRecurrenceEndDate())) {
            LocalDate adjustedDate = DateUtils.adjustDateForMonth(dayOfMonth, date);
            createDayEventSlot(dayEventRequest, dayEvent, adjustedDate);
            date = date.plus(dayEvent.getRecurrenceStep(), unit);
        }
    }

    // Same logic as the above method. We have to handle last day of month case.
    private void createNOccurrencesSameDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent, ChronoUnit unit) {
        if (dayEvent.getRecurrenceDuration() != RecurrenceDuration.N_OCCURRENCES) {
            return;
        }

        int dayOfMonth = dayEvent.getStartDate().getDayOfMonth();
        LocalDate startDate = dayEvent.getStartDate();
        for (int i = 0; i <= dayEvent.getNumberOfOccurrences(); i++) {
            startDate = DateUtils.adjustDateForMonth(dayOfMonth, startDate);
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            startDate = startDate.plus(dayEvent.getRecurrenceStep(), unit);
        }
    }

    private void createDayEventSlot(DayEventRequest eventRequest, DayEvent dayEvent, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate()));
        DayEventSlot eventSlot = DayEventSlot.builder()
                .id(UUID.randomUUID())
                .eventId(dayEvent.getId())
                .startDate(startDate)
                .endDate(endDate)
                .title(eventRequest.getTitle())
                .description(eventRequest.getDescription())
                .location(eventRequest.getLocation())
                .guestEmails(eventRequest.getGuestEmails())
                .build();
        this.eventSlotRepository.create(eventSlot);
    }

    /*
        It returns according to TemporalUnit.java: the amount of time between temporal1Inclusive and
        temporal2Exclusive in terms of this unit; positive if temporal2Exclusive is later than
        temporal1Inclusive, negative if earlier. In our case, temporal1Inclusive is startDate and
        temporal2Exclusive is endDate.

        For example, March 15 - March 17, the return value of ChronoUnit.DAYS.between(dayEvent.getStartDate(), dayEvent.getEndDate())
        will return 2, March 15 to March 16: 1 day and March 16 to March 17: 1 day. Every event day slot
        will be created with 2 days between start and end date which is the correct interval given the
        initial event. It also considers leap years.
    */
    private int getEventDuration(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }
}
