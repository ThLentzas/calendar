package org.example.google_calendar_clone.calendar.event.time.slot;

import org.example.google_calendar_clone.calendar.event.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTOConverter;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.utils.DateUtils;
import org.springframework.stereotype.Service;

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
public class TimeEventSlotService implements IEventSlotService<TimeEventRequest, TimeEvent, TimeEventSlotDTO> {
    private final TimeEventSlotRepository timeEventSlotRepository;
    private static final TimeEventSlotDTOConverter converter = new TimeEventSlotDTOConverter();

    /*
        For every time event slot, we convert the start/end time to UTC according to their respective timezones. All
        the times stored in the DB will be in UTC. When we return an event slot to the user we convert the UTC time
        back to their local time according to the timezones we stored alongside them. This happens in the converter
     */
    @Override
    public void create(TimeEventRequest eventRequest, TimeEvent event) {
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
                createUntilDateEventSlots(eventRequest, event, ChronoUnit.DAYS);
                createNRepetitionsEventSlots(eventRequest, event, ChronoUnit.DAYS);
            }
            case WEEKLY -> {
                createUntilDateEventSlots(eventRequest, event, ChronoUnit.WEEKS);
                createNRepetitionsEventSlots(eventRequest, event, ChronoUnit.WEEKS);
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
            case ANNUALLY -> {
                createUntilDateSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
                createNRepetitionsSameDayEventSlots(eventRequest, event, ChronoUnit.YEARS);
            }
        }
    }

    @Override
    public List<TimeEventSlotDTO> findEventSlotsByEventId(UUID eventId) {
        return this.timeEventSlotRepository.findByEventId(eventId)
                .stream()
                .map(converter::convert)
                .toList();
    }

    /*
        In the plus() method we can pass a unit of time to increase our date in the loop. According to the unit passed
        plusWeeks(), plusDays() etc will be called. It is a way to avoid having a different case for each value

        date = date.plusDays(), date = date.plusWeeks() etc
     */
    private void createUntilDateEventSlots(TimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (event.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && event.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        LocalDateTime dateTime = event.getStartTime();
        while (!dateTime.toLocalDate().isAfter(event.getRepetitionEndDate())) {
            createTimeEventSlot(eventRequest, event, dateTime);
            dateTime = dateTime.plus(event.getRepetitionStep(), unit);
        }
    }

    private void createNRepetitionsEventSlots(TimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        LocalDateTime startTime = event.getStartTime();
        for (int i = 0; i <= event.getRepetitionCount(); i++) {
            createTimeEventSlot(eventRequest, event, startTime);
           /*
                The starDate is updated to the previous value plus the number of the repetition step which
                can be 1, 2 etc, meaning the event is to be repeated every 1,2, days until we reach
                N_REPETITIONS
            */
            startTime = startTime.plus(event.getRepetitionStep(), unit);
        }
    }

    // Monthly events that repeat the same week day until a certain date(2nd Tuesday of the month)
    private void createUntilDateMonthlySameWeekDayEventSlots(TimeEventRequest eventRequest, TimeEvent event) {
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
    private void createNRepetitionsMonthlySameWeekDayEventSlots(TimeEventRequest eventRequest, TimeEvent event) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int occurrences = DateUtils.findDayOfMonthOccurrence(LocalDate.from(event.getStartTime()));
        LocalDate startDate = LocalDate.from(event.getStartTime());
        LocalDateTime startTime = event.getStartTime();
        for (int i = 0; i <= event.getRepetitionCount(); i++) {
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

    private void createUntilDateSameDayEventSlots(TimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
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

    private void createNRepetitionsSameDayEventSlots(TimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (event.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        int dayOfMonth = event.getStartTime().getDayOfMonth();
        LocalDateTime startTime = event.getStartTime();
        for (int i = 0; i <= event.getRepetitionCount(); i++) {
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
        accordingly. The end time in UTC is the start time in UTC plus the event duration.
     */
    private void createTimeEventSlot(TimeEventRequest eventRequest, TimeEvent event, LocalDateTime startTime) {
        startTime = DateUtils.convertToUTC(startTime, eventRequest.getStartTimeZoneId());
        LocalDateTime endTime = startTime.plusMinutes(getEventDuration(event.getStartTime(), eventRequest.getEndTime()));

        TimeEventSlot timeEventSlot = new TimeEventSlot();
        timeEventSlot.setStartTime(startTime);
        timeEventSlot.setEndTime(endTime);
        timeEventSlot.setStartTimeZoneId(eventRequest.getStartTimeZoneId());
        timeEventSlot.setEndTimeZoneId(eventRequest.getEndTimeZoneId());
        timeEventSlot.setName(eventRequest.getName());
        timeEventSlot.setDescription(eventRequest.getDescription());
        timeEventSlot.setLocation(eventRequest.getLocation());
        timeEventSlot.setGuestEmails(eventRequest.getGuestEmails());
        timeEventSlot.setTimeEvent(event);
        this.timeEventSlotRepository.save(timeEventSlot);
    }

    private int getEventDuration(LocalDateTime startTime, LocalDateTime endTime) {
        return (int) ChronoUnit.MINUTES.between(startTime, endTime);
    }
}