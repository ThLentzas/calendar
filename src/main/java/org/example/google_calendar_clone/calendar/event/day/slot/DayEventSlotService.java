package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.calendar.event.day.DayEventRepository;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.slot.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTOConverter;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.example.google_calendar_clone.utils.DateUtils;
import org.example.google_calendar_clone.utils.EventUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventSlotService implements IEventSlotService<DayEventRequest, DayEvent, DayEventSlotDTO> {
    private final DayEventSlotRepository dayEventSlotRepository;
    private final DayEventRepository dayEventRepository;
    private final UserRepository userRepository;
    private static final DayEventSlotDTOConverter converter = new DayEventSlotDTOConverter();
    private static final String EVENT_SLOT_NOT_FOUND_MSG = "Day event slot not found with id: ";

    /*
        For every event we need to precompute the future day events as long as it repeats. In the DAILY case
        we start from the start date of the event request and, we need to compute every event until the
        repetition end date (when the repeating event should stop). As long as the start date has not reached
        the repetition end date we create a day event slot, where the start date will be the current date and
        the end date will be equal to the interval of the initial startDate - endDate (The duration of the event).
        This is calculated using ChronoUnit.DAYS.between(dayEvent.getStartDate(), dayEvent.getEndDate()).
        Lastly, we need to skip ahead equal to the number of the repetition step (every 2 days/weeks/months etc).

        We create the DayEventSlots based on the DayEvent, and we retrieve properties like dayEvent.getStartDate();
        and not like dayEventRequest.getStartDate(). The values are the same. That was my thought process
    */
    @Override
    public void create(DayEventRequest eventRequest, DayEvent event) {
        // We want to send invitation emails only to emails that at least contain @
        Set<String> guestEmails = new HashSet<>();
        if (eventRequest.getGuestEmails() != null) {
            guestEmails = eventRequest.getGuestEmails().stream()
                    .filter(guestEmail -> guestEmail.contains("@"))
                    .collect(Collectors.toSet());
        }
        eventRequest.setGuestEmails(guestEmails);

        switch (event.getRepetitionFrequency()) {
            // Since the event is not repeating, we only create 1 DayEventSlot
            case NEVER -> createDayEventSlot(eventRequest, event, event.getStartDate());
            case DAILY -> {
                createUntilDateDailyEventSlots(eventRequest, event);
                createNRepetitionsDailyEventSlots(eventRequest, event);
            }
            case WEEKLY -> {
                createUntilDateWeeklyEventSlots(eventRequest, event);
                createNRepetitionsWeeklyEventSlots(eventRequest, event);
            }
            case MONTHLY -> {
                if (event.getMonthlyRepetitionType().equals(MonthlyRepetitionType.SAME_WEEKDAY)) {
                    createUntilDateMonthlySameWeekDayEventSlots(eventRequest, event);
                    createNRepetitionsMonthlySameWeekDayEventSlots(eventRequest, event);
                } else {
                    // For events that are repeating Monthly, over N Months, Annually, or over N years we will use the
                    // same method to take into consideration things like leap years for events at 29 February and
                    // events that are to be repeated at the last day of the month
                    createUntilDateSameDayEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                    createNRepetitionsSameDayEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                }
            }
            /*
                For annually repeating events, apart from 29th of February, all we need to know is that months have
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
                createNRepetitionsSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
            }
        }
    }

    /*
        We can not call getReferenceById(), we need the email.

        There are 2 cases where the existsByEventIdAndUserId() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        We cover both with our existsByEventIdAndUserId(). If the event exists and the user is not organizer it returns
        false. If the event does not exist it also returns false. In theory, the user should exist in our database,
        because we use the id of the current authenticated user. There is also an argument for data integrity problems,
        where the user was deleted and the token was not invalidated.
     */
    @Override
    public void inviteGuests(Long userId, UUID slotId, InviteGuestsRequest inviteGuestsRequest) {
        DayEventSlot eventSlot = this.dayEventSlotRepository.findByIdOrThrow(slotId);
        if (!this.dayEventRepository.existsByEventIdAndUserId(eventSlot.getDayEvent().getId(), userId)) {
            throw new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId);
        }

        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        Set<String> guestEmails = EventUtils.processGuestEmails(user, inviteGuestsRequest, eventSlot.getGuestEmails());
        eventSlot.getGuestEmails().addAll(guestEmails);

        // The method is @Transactional, but we explicitly call save() to know that we update the guest list
        this.dayEventSlotRepository.save(eventSlot);
    }

    /*
        Returns an event slot where the user is either the organizer or invited as guest
     */
    @Override
    public List<DayEventSlotDTO> findEventSlotsByEventId(UUID eventId) {
        return this.dayEventSlotRepository.findByEventId(eventId)
                .stream()
                .map(converter::convert)
                .toList();
    }

    @Override
    public DayEventSlotDTO findByUserAndSlotId(Long userId, UUID slotId) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        DayEventSlot eventSlot = this.dayEventSlotRepository.findByUserIdAndSlotId(
                user.getId(),
                user.getEmail(),
                slotId).orElseThrow(() -> new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId));

        return converter.convert(eventSlot);
    }

    public List<DayEventSlotDTO> findEventSlotsByUserInDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return this.dayEventSlotRepository.findByUserInDateRange(user.getId(), user.getEmail(), startDate, endDate)
                .stream()
                .map(converter::convert)
                .toList();
    }

    private void createUntilDateDailyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && dayEvent.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRepetitionEndDate())) {
            createDayEventSlot(dayEventRequest, dayEvent, date);
            date = date.plusDays(dayEvent.getRepetitionStep());
        }
    }

    private void createNRepetitionsDailyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        LocalDate startDate = dayEvent.getStartDate();
        for (int i = 0; i < dayEvent.getRepetitionOccurrences(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
           /*
                The starDate is updated to the previous value plus the number of the repetition step which
                can be 1, 2 etc, meaning the event is to be repeated every 1,2, days until we reach
                N_REPETITIONS
            */
            startDate = startDate.plusDays(dayEvent.getRepetitionStep());
        }
    }

    /*
        Weekly events can be repeated on certain days of week. ["MONDAY", "THURSDAY", "SATURDAY"]. In the request,
        one of those 3 days will correspond to the date provided by the user. We have 2 cases to consider:
            Case 1: The next day to be repeated is after the day that corresponds to the start date. For example,
            if 2024-09-12 is a Thursday and we want the event to be repeated on Monday, Monday is before Thursday so,
            we need to adjust the date. We need to find the difference between the day of the start date and the day
            to be repeated in our case: Thursday - Monday (in ordinal enum values) will give us a positive difference
            which means the day of the start date is after the day to be repeated, so we need to subtract days from the
            startDate (startDate = date.minus(difference)), Thursday - Monday = 3 in ordinal,
            startDate = date.minus(3) would result in 2024-09-09 which is a Monday, but this is in the past relative
            to our event. In this case, we need to find the next Monday, this is why the check is there
            !startDate.isBefore(dayEventRequest.getStartDate()). We are interested in future occurrences relative to
            our eventRequest.getStatDate()

            Case 2: Opposite of 1. We need to add the difference but since the number will be negative will add its
            absolute value

            @Test
            void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNWeeksUntilACertainDate(), DayEventSlotServiceTest
     */
    private void createUntilDateWeeklyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && dayEvent.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        LocalDate startDate;
        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRepetitionEndDate())) {
            for (DayOfWeek dayOfWeek : dayEvent.getWeeklyRecurrenceDays()) {
                int differenceInDays = dayEvent.getStartDate().getDayOfWeek().getValue() - dayOfWeek.getValue();
                if (differenceInDays > 0) {
                    startDate = date.minusDays(differenceInDays);
                } else {
                    // Math.abs() would also, we are using the minus operator, which negates the integer(changes the sing)
                    startDate = date.plusDays(-differenceInDays);
                }

                // The start date is within the repetition end date
                if (!startDate.isBefore(dayEvent.getStartDate()) && !startDate.isAfter(dayEvent.getRepetitionEndDate())) {
                    createDayEventSlot(dayEventRequest, dayEvent, startDate);
                }
            }
            date = date.plusWeeks(dayEvent.getRepetitionStep());
        }
    }

    /*
        Similar logic with createUntilDateWeeklyEventSlots()

            @Test
            void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNWeeksForNRepetitions(), DayEventSlotServiceTest
     */
    private void createNRepetitionsWeeklyEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int count = 0;
        LocalDate startDate;
        LocalDate date = dayEvent.getStartDate();
        while (count < dayEvent.getRepetitionOccurrences()) {
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
                    if (count == dayEvent.getRepetitionOccurrences()) {
                        return;
                    }
                }
            }
            date = date.plusWeeks(dayEvent.getRepetitionStep());
        }
    }

    // Monthly events that repeat the same week day until a certain date(2nd Tuesday of the month)
    private void createUntilDateMonthlySameWeekDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && dayEvent.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        int occurrences = DateUtils.findDayOfMonthOccurrence(dayEvent.getStartDate());
        LocalDate startDate;
        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRepetitionEndDate())) {
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(
                    YearMonth.of(date.getYear(), date.getMonth()),
                    dayEvent.getStartDate().getDayOfWeek(),
                    occurrences
            );
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            date = date.plusMonths(dayEvent.getRepetitionStep());
        }
    }

    // Monthly events that repeat the same week day until a number of repetitions(2nd Tuesday of the month)
    private void createNRepetitionsMonthlySameWeekDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }
        int occurrences = DateUtils.findDayOfMonthOccurrence(dayEvent.getStartDate());
        LocalDate startDate = dayEvent.getStartDate();
        for (int i = 0; i < dayEvent.getRepetitionOccurrences(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            startDate = startDate.plusMonths(dayEvent.getRepetitionStep());
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(
                    YearMonth.of(startDate.getYear(), startDate.getMonth()),
                    dayEvent.getStartDate().getDayOfWeek(),
                    occurrences
            );
        }
    }

    /*
        When it comes to monthly repeated events on the same day we have to consider an edge case where we want the
        event to be repeated at the last day of each month for 14 months. If we have an event for the 31 of January
        we can't move to the 31st of February, it is not a valid date. We need to move to the last day of each month.
        Java takes care of the advancing part, where if we are at 31st of January and, we say date = date.plusMonths(1)
        it will take us to 28 or 29 of February depending on if it is a leap year or not. The problem is that when we
        advance again we will move to the 28 or 29 of March which is not the last day of the month. We have to address
        that scenario.
        Logic also explained to the DateUtils.adjustDateForMonth()
     */
    private void createUntilDateSameDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent, ChronoUnit unit) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && dayEvent.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        /*
            int dayOfMonth = dayEvent.getStartDate().getDayOfMonth();
            for (LocalDate date = dayEvent.getStartDate(); !date.isAfter(dayEvent.getRepetitionEndDate());
                 date = date.plusMonths(dayEvent.getRepetitionStep())) {
                date = DateUtils.adjustDateForMonth(dayOfMonth, date);
                createDayEventSlot(dayEventRequest, dayEvent, date);
            }

            The above code would work but the linter would complain as : Refactor the code in order to not assign to this
            loop counter from within the loop body. We increment the months of the date and also adjust the date of
            the month.
         */
        int dayOfMonth = dayEvent.getStartDate().getDayOfMonth();
        LocalDate date = dayEvent.getStartDate();
        while (!date.isAfter(dayEvent.getRepetitionEndDate())) {
            LocalDate adjustedDate = DateUtils.adjustDateForMonth(dayOfMonth, date);
            createDayEventSlot(dayEventRequest, dayEvent, adjustedDate);
            date = date.plus(dayEvent.getRepetitionStep(), unit);
        }
    }

    // Same logic as the above method. We have to handle last day of month case.
    private void createNRepetitionsSameDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent, ChronoUnit unit) {
        if (dayEvent.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int dayOfMonth = dayEvent.getStartDate().getDayOfMonth();
        LocalDate startDate = dayEvent.getStartDate();
        for (int i = 0; i <= dayEvent.getRepetitionOccurrences(); i++) {
            startDate = DateUtils.adjustDateForMonth(dayOfMonth, startDate);
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            startDate = startDate.plus(dayEvent.getRepetitionStep(), unit);
        }
    }

    private void createDayEventSlot(DayEventRequest dayEventRequest, DayEvent dayEvent, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate()));
        DayEventSlot dayEventSlot = new DayEventSlot();
        dayEventSlot.setStartDate(startDate);
        dayEventSlot.setEndDate(endDate);
        dayEventSlot.setName(dayEventRequest.getName());
        dayEventSlot.setDescription(dayEventRequest.getDescription());
        dayEventSlot.setLocation(dayEventRequest.getLocation());
        dayEventSlot.setGuestEmails(dayEventRequest.getGuestEmails());
        dayEventSlot.setDayEvent(dayEvent);
        this.dayEventSlotRepository.save(dayEventSlot);
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
