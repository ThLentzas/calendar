package org.example.google_calendar_clone.calendar.event.time.slot;

import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.slot.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.time.TimeEventRepository;
import org.example.google_calendar_clone.calendar.event.time.dto.CreateTimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.dto.UpdateTimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTOConverter;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.example.google_calendar_clone.utils.DateUtils;
import org.example.google_calendar_clone.utils.EventUtils;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class TimeEventSlotService implements IEventSlotService<CreateTimeEventRequest, UpdateTimeEventRequest, TimeEvent, TimeEventSlotDTO> {
    private final TimeEventSlotRepository timeEventSlotRepository;
    private final TimeEventRepository timeEventRepository;
    private final UserRepository userRepository;
    private static final TimeEventSlotDTOConverter converter = new TimeEventSlotDTOConverter();
    private static final String EVENT_SLOT_NOT_FOUND_MSG = "Time event slot not found with id: ";

    /*
        For every time event slot, we convert the start/end time to UTC according to their respective timezones. All
        the times stored in the DB will be in UTC. When we return an event slot to the user we convert the UTC time
        back to their local time according to the timezones we stored alongside them. This happens in the converter
     */
    @Override
    public void create(CreateTimeEventRequest eventRequest, TimeEvent event) {
        // We want to send invitation emails only to emails that at least contain @
        Set<String> guestEmails = new HashSet<>();
        if (eventRequest.getGuestEmails() != null) {
            guestEmails = eventRequest.getGuestEmails().stream()
                    .filter(guestEmail -> guestEmail.contains("@"))
                    .collect(Collectors.toSet());
        }
        eventRequest.setGuestEmails(guestEmails);

        switch (event.getRepetitionFrequency()) {
            // Since the event is not repeating, we only create 1 TimeEventSlot
            case NEVER -> createTimeEventSlot(eventRequest, event, event.getStartTime());
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
                    createUntilDateMonthlySameWeekdayEventSlots(eventRequest, event);
                    createNRepetitionsMonthlySameWeekdayEventSlots(eventRequest, event);
                } else {
                    // For events that are repeating Monthly, over N Months, Annually, or over N years we will use the
                    // same method to take into consideration things like leap years for events at 29 February and
                    // events that are to be repeated at the last day of the month
                    createUntilDateSameDayEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                    createNRepetitionsSameDayEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                }
            }
            case ANNUALLY -> {
                createUntilDateSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
                createNRepetitionsSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
            }
        }
    }

    /*
        This method will be tested via an Integration test, because EventUtils.hasSameFrequencyDetails() is already
        tested and the remaining code is just calling delete() and save(). No logic to be tested in the
        TimeEventSlotServiceTest class, create is already fully tested.
     */
    @Override
    public void update(UpdateTimeEventRequest eventRequest, TimeEvent event) {
        if (eventRequest.getRepetitionFrequency() != null && !EventUtils.hasSameFrequencyDetails(eventRequest, event)) {
            CreateTimeEventRequest createTimeEventRequest = CreateTimeEventRequest.builder()
                    .title(eventRequest.getTitle())
                    .location(eventRequest.getLocation())
                    .description(eventRequest.getDescription())
                    .guestEmails(eventRequest.getGuestEmails())
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
                    .build();

            event.setStartTime(eventRequest.getStartTime());
            event.setEndTime(eventRequest.getEndTime());
            event.setStartTimeZoneId(eventRequest.getStartTimeZoneId());
            event.setEndTimeZoneId(eventRequest.getEndTimeZoneId());
            EventUtils.setFrequencyDetails(eventRequest, event);

            this.timeEventSlotRepository.deleteAll(event.getTimeEventSlots());

            // Maybe this is bad practise to self invoke public methods, but we need to create the event slots for the different frequency
            create(createTimeEventRequest, event);
        } else {
            event.getTimeEventSlots().forEach(eventSlot -> {
                eventSlot.setTitle(eventRequest.getTitle() != null
                        && !eventRequest.getTitle().isBlank() ? eventRequest.getTitle() : eventSlot.getTitle());
                eventSlot.setLocation(eventRequest.getLocation() != null
                        && !eventRequest.getLocation().isBlank() ? eventRequest.getLocation()
                        : eventSlot.getLocation());
                eventSlot.setDescription(eventRequest.getDescription() != null
                        && !eventRequest.getDescription().isBlank() ? eventRequest.getDescription()
                        : eventSlot.getDescription());
                eventSlot.setGuestEmails(eventRequest.getGuestEmails() != null
                        && !eventRequest.getGuestEmails().isEmpty() ? eventRequest.getGuestEmails()
                        : eventSlot.getGuestEmails());

                this.timeEventSlotRepository.save(eventSlot);
            });
        }
    }

    @Override
    public void inviteGuests(Long userId, UUID slotId, InviteGuestsRequest inviteGuestsRequest) {
        TimeEventSlot eventSlot = this.timeEventSlotRepository.findByIdOrThrow(slotId);
        if (!this.timeEventRepository.existsByEventIdAndUserId(eventSlot.getTimeEvent().getId(), userId)) {
            throw new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId);
        }

        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        Set<String> guestEmails = EventUtils.processGuestEmails(user, inviteGuestsRequest, eventSlot.getGuestEmails());
        eventSlot.getGuestEmails().addAll(guestEmails);

        // The method is @Transactional, but we explicitly call save() to know that we update the guest list
        this.timeEventSlotRepository.save(eventSlot);
    }

    @Override
    public List<TimeEventSlotDTO> findEventSlotsByEventId(UUID eventId) {
        return this.timeEventSlotRepository.findByEventId(eventId)
                .stream()
                .map(converter::convert)
                .toList();
    }

    @Override
    public TimeEventSlotDTO findByUserAndSlotId(Long userId, UUID slotId) {
        User user = this.userRepository.findAuthUserByIdOrThrow(userId);
        TimeEventSlot eventSlot = this.timeEventSlotRepository.findByUserAndSlotId(
                user.getId(),
                user.getEmail(),
                slotId).orElseThrow(() -> new ResourceNotFoundException(EVENT_SLOT_NOT_FOUND_MSG + slotId));

        return converter.convert(eventSlot);
    }

    public List<TimeEventSlotDTO> findEventSlotsByUserInDateRange(User user,
                                                                  LocalDateTime startTime,
                                                                  LocalDateTime endTime) {
        return this.timeEventSlotRepository.findByUserInDateRange(user, user.getEmail(), startTime, endTime)
                .stream()
                .map(converter::convert)
                .toList();
    }

    /*
        In the plus() method we can pass a unit of time to increase our date in the loop. According to the unit passed
        plusWeeks(), plusDays() etc will be called. It is a way to avoid having a different case for each value

        date = date.plusDays(), date = date.plusWeeks() etc
     */
    private void createUntilDateDailyEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && event.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        LocalDateTime dateTime = event.getStartTime();
        while (!dateTime.toLocalDate().isAfter(event.getRepetitionEndDate())) {
            createTimeEventSlot(eventRequest, event, dateTime);
            dateTime = dateTime.plusDays(event.getRepetitionStep());
        }
    }

    private void createNRepetitionsDailyEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        LocalDateTime startTime = event.getStartTime();
        for (int i = 0; i <= event.getRepetitionOccurrences(); i++) {
            createTimeEventSlot(eventRequest, event, startTime);
           /*
                The starDate is updated to the previous value plus the number of the repetition step which
                can be 1, 2 etc, meaning the event is to be repeated every 1,2, days until we reach
                N_REPETITIONS
            */
            startTime = startTime.plusDays(event.getRepetitionStep());
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
            void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNWeeksUntilACertainDate(), TimeEventSlotServiceTest
     */
    private void createUntilDateWeeklyEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && event.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        LocalDate startDate;
        LocalDateTime startTime;
        LocalDateTime dateTime = event.getStartTime();
        while (!dateTime.toLocalDate().isAfter(event.getRepetitionEndDate())) {
            for (DayOfWeek dayOfWeek : event.getWeeklyRecurrenceDays()) {
                int differenceInDays = event.getStartTime().getDayOfWeek().getValue() - dayOfWeek.getValue();
                if (differenceInDays > 0) {
                    startDate = LocalDate.from(dateTime.minusDays(differenceInDays));
                } else {
                    // Math.abs() would also, we are using the minus operator, which negates the integer(changes the sing)
                    startDate = LocalDate.from(dateTime).plusDays(-differenceInDays);
                }

                // The start date is within the repetition end date
                if (!startDate.isBefore(event.getStartTime().toLocalDate()) && !startDate.isAfter(event.getRepetitionEndDate())) {
                    startTime = startDate.atTime(event.getStartTime().toLocalTime());
                    createTimeEventSlot(eventRequest, event, startTime);
                }
            }
            dateTime = dateTime.plusWeeks(event.getRepetitionStep());
        }
    }

    /*
        Similar logic with createUntilDateWeeklyEventSlots()

            @Test
            void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNWeeksForNRepetitions(), TimeEventSlotServiceTest
     */
    private void createNRepetitionsWeeklyEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int count = 0;
        LocalDate startDate;
        LocalDateTime startTime;
        LocalDateTime dateTime = event.getStartTime();
        while (count < event.getRepetitionOccurrences()) {
            for (DayOfWeek dayOfWeek : event.getWeeklyRecurrenceDays()) {
                int differenceInDays = event.getStartTime().getDayOfWeek().getValue() - dayOfWeek.getValue();
                if (differenceInDays > 0) {
                    startDate = LocalDate.from(dateTime.minusDays(differenceInDays));
                } else {
                    // Math.abs() would also, we are using the minus operator, which negates the integer(changes the sing)
                    startDate = LocalDate.from(dateTime).plusDays(-differenceInDays);
                }
                if (!startDate.isBefore(event.getStartTime().toLocalDate())) {
                    startTime = startDate.atTime(event.getStartTime().toLocalTime());
                    createTimeEventSlot(eventRequest, event, startTime);
                    count++;
                    // During the inner loop the count might be equal or greater than the occurrences
                    if (count == event.getRepetitionOccurrences()) {
                        return;
                    }
                }
            }
            dateTime = dateTime.plusWeeks(event.getRepetitionStep());
        }
    }

    // Monthly events that repeat the same week day until a certain date(2nd Tuesday of the month)
    private void createUntilDateMonthlySameWeekdayEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && event.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        int occurrences = DateUtils.findDayOfMonthOccurrence(LocalDate.from(event.getStartTime()));
        LocalDate startDate;
        LocalDateTime startTime;
        LocalDateTime dateTime = event.getStartTime();
        while (!dateTime.toLocalDate().isAfter(event.getRepetitionEndDate())) {
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(
                    YearMonth.of(dateTime.getYear(), dateTime.getMonth()),
                    event.getStartTime().getDayOfWeek(),
                    occurrences
            );
            // Combines this date with a time to create a LocalDateTime. Returns LocalDateTime formed from this date at
            // the specified time.
            startTime = startDate.atTime(event.getStartTime().toLocalTime());
            createTimeEventSlot(eventRequest, event, startTime);
            dateTime = dateTime.plusMonths(event.getRepetitionStep());
        }
    }

    // Monthly events that repeat the same week day until a number of repetitions(2nd Tuesday of the month)
    private void createNRepetitionsMonthlySameWeekdayEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int occurrences = DateUtils.findDayOfMonthOccurrence(LocalDate.from(event.getStartTime()));
        LocalDate startDate = LocalDate.from(event.getStartTime());
        LocalDateTime startTime = event.getStartTime();
        for (int i = 0; i <= event.getRepetitionOccurrences(); i++) {
            createTimeEventSlot(eventRequest, event, startTime);
            startDate = LocalDate.from(startDate).plusMonths(event.getRepetitionStep());
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(
                    YearMonth.of(startDate.getYear(), startDate.getMonth()),
                    event.getStartTime().getDayOfWeek(),
                    occurrences
            );
            // We want the upcoming events to start the same time as the original one. We find the next date and add the time
            startTime = startDate.atTime(event.getStartTime().toLocalTime());
        }
    }

    private void createUntilDateSameDayEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (event.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && event.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        int dayOfMonth = event.getStartTime().getDayOfMonth();
        LocalDateTime dateTime = event.getStartTime();
        while (!dateTime.toLocalDate().isAfter(event.getRepetitionEndDate())) {
            LocalDate adjustedDate = DateUtils.adjustDateForMonth(dayOfMonth, dateTime.toLocalDate());
            LocalDateTime adjustedDateTime = adjustedDate.atTime(dateTime.toLocalTime());
            createTimeEventSlot(eventRequest, event, adjustedDateTime);

            dateTime = dateTime.plus(event.getRepetitionStep(), unit);
        }
    }

    private void createNRepetitionsSameDayEventSlots(CreateTimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int dayOfMonth = event.getStartTime().getDayOfMonth();
        LocalDateTime startTime = event.getStartTime();
        for (int i = 0; i <= event.getRepetitionOccurrences(); i++) {
            LocalDate adjustedDate = DateUtils.adjustDateForMonth(dayOfMonth, startTime.toLocalDate());
            LocalDateTime adjustedStartTime = adjustedDate.atTime(startTime.toLocalTime());
            createTimeEventSlot(eventRequest, event, adjustedStartTime);

            startTime = startTime.plus(event.getRepetitionStep(), unit);
        }
    }

    /*
        The user provides the start time and end time to their preferred timezone. We are storing the start time and
        end time to UTC. When we will display the event to the user, we can convert back to the user's timezone that we
        stored along the start and end time. This way we have consistency in the times stored in our db and we can adjust
        accordingly. The end time in UTC is the start time in UTC plus the event duration calculated aware of different
        timezones.

        Let's consider these 2 dates:
            2024-09-12 10:00 AM EDT
            2024-09-12 4:00 PM CEST

            Without taking timezones into consideration, the duration of the event would be 6 hours, but this not the
            case. If both are converted to UTC, the difference is 0, both are 14:00 UTC. This is why we need to consider
            timezones for the event duration
     */
    private void createTimeEventSlot(CreateTimeEventRequest eventRequest, TimeEvent event, LocalDateTime startTime) {
        startTime = DateUtils.convertToUTC(startTime, eventRequest.getStartTimeZoneId());
        LocalDateTime endTime = startTime.plusMinutes(DateUtils.timeZoneAwareDifference(
                event.getStartTime(),
                event.getStartTimeZoneId(),
                event.getEndTime(),
                event.getEndTimeZoneId(),
                ChronoUnit.MINUTES));

        TimeEventSlot timeEventSlot = new TimeEventSlot();
        timeEventSlot.setStartTime(startTime);
        timeEventSlot.setEndTime(endTime);
        timeEventSlot.setStartTimeZoneId(event.getStartTimeZoneId());
        timeEventSlot.setEndTimeZoneId(event.getEndTimeZoneId());
        timeEventSlot.setTitle(eventRequest.getTitle());
        timeEventSlot.setDescription(eventRequest.getDescription());
        timeEventSlot.setLocation(eventRequest.getLocation());
        timeEventSlot.setGuestEmails(eventRequest.getGuestEmails());
        timeEventSlot.setTimeEvent(event);
        this.timeEventSlotRepository.save(timeEventSlot);
    }
}