package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.calendar.event.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotConverter;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.exception.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventSlotService implements IEventSlotService<DayEventRequest, DayEvent, DayEventSlotDTO> {
    private final DayEventSlotRepository dayEventSlotRepository;
    private static final DayEventSlotConverter dayEventSlotConverter = new DayEventSlotConverter();
    private static final Logger logger = LoggerFactory.getLogger(DayEventSlotService.class);

    /*
        For every event we need to precompute the future day events as long as it repeats. In the DAILY case
        we start from the start date of the event request and, we need to compute every event until the
        repetition end date (when the repeating event should stop). As long as the start date has not reached
        the repetition end date we create a day event slot, where the start date will be the current date and
        the end date will be equal to the interval of the initial startDate - endDate (The duration of the event).
        This is calculated using ChronoUnit.DAYS.between(dayEvent.getStartDate(), dayEvent.getEndDate()).
        Lastly, we need to skip ahead equal to the number of the repetition step (every 2 days/weeks/months etc).
    */
    @Override
    public void create(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        switch (dayEventRequest.getRepetitionFrequency()) {
            // Since the event is not to be repeated, we only create 1 DayEventSlot
            case NEVER -> createDayEventSlot(dayEventRequest, dayEvent, dayEventRequest.getStartDate());
            case DAILY -> {
                createUntilDateEventSlots(dayEventRequest, dayEvent, ChronoUnit.DAYS);
                createNRepetitionsEventSlots(dayEventRequest, dayEvent, ChronoUnit.DAYS);
            }
            case WEEKLY -> {
                createUntilDateEventSlots(dayEventRequest, dayEvent, ChronoUnit.WEEKS);
                createNRepetitionsEventSlots(dayEventRequest, dayEvent, ChronoUnit.WEEKS);
            }
            case MONTHLY -> {
                if (dayEventRequest.getMonthlyRepetitionType().equals(MonthlyRepetitionType.SAME_WEEKDAY)) {
                    createUntilDateMonthlySameWeekDayEventSlots(dayEventRequest, dayEvent);
                    createNRepetitionsMonthlySameWeekDayEventSlots(dayEventRequest, dayEvent);
                } else {
                    createUntilDateEventSlots(dayEventRequest, dayEvent, ChronoUnit.MONTHS);
                    createNRepetitionsEventSlots(dayEventRequest, dayEvent, ChronoUnit.MONTHS);
                }
            }
            case ANNUALLY -> {
                createUntilDateEventSlots(dayEventRequest, dayEvent, ChronoUnit.YEARS);
                createNRepetitionsEventSlots(dayEventRequest, dayEvent, ChronoUnit.YEARS);
            }
        }
    }

    @Override
    public List<DayEventSlotDTO> findEventSlotsByEventId(UUID eventId) {
        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(eventId);

        return dayEventSlots.stream()
                .map(dayEventSlotConverter::convert)
                .toList();
    }

    /*
        In the plus() method we can pass a unit of time to increase our date in the loop. According to the unit passed
        plusWeeks(), plusDays() etc will be called. It is a way to avoid having a different case for each value

        date = date.plusDays(), date = date.plusWeeks() etc
     */
    private void createUntilDateEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent, ChronoUnit unit) {
        if (dayEventRequest.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && dayEventRequest.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        for (LocalDate date = dayEvent.getStartDate(); !date.isAfter(dayEvent.getRepetitionEndDate());
             date = date.plus(dayEvent.getRepetitionStep(), unit)) {
            createDayEventSlot(dayEventRequest, dayEvent, date);
        }
    }

    private void createNRepetitionsEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent, ChronoUnit unit) {
        if (dayEventRequest.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        LocalDate startDate = dayEventRequest.getStartDate();
        for (int i = 0; i <= dayEventRequest.getRepetitionCount(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
           /*
                The starDate is updated to the previous value plus the number of the repetition step which
                can be 1, 2 etc, meaning the event is to be repeated every 1,2, days until we reach
                N_REPETITIONS
            */
            startDate = startDate.plus(dayEventRequest.getRepetitionStep(), unit);
        }
    }

    // Monthly events that repeat the same week day until a certain date(2nd Tuesday of the month)
    private void createUntilDateMonthlySameWeekDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEventRequest.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && dayEventRequest.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        int occurrences = findDayOfMonthOccurrence(dayEventRequest);
        LocalDate startDate;
        for (LocalDate date = dayEventRequest.getStartDate(); !date.isAfter(dayEventRequest.getRepetitionEndDate()); date = date.plusMonths(dayEventRequest.getRepetitionStep())) {
            startDate = findDateOfNthDayOfWeekInMonth(YearMonth.of(date.getYear(), date.getMonth()), dayEventRequest.getStartDate().getDayOfWeek(), occurrences);
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
        }
    }

    // Monthly events that repeat the same week day until a number of repetitions(2nd Tuesday of the month)
    private void createNRepetitionsMonthlySameWeekDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEventRequest.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }
        int occurrences = findDayOfMonthOccurrence(dayEventRequest);
        LocalDate startDate = dayEventRequest.getStartDate();
        for (int i = 0; i <= dayEventRequest.getRepetitionCount(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            startDate = startDate.plusMonths(dayEventRequest.getRepetitionStep());
            startDate = findDateOfNthDayOfWeekInMonth(YearMonth.of(startDate.getYear(), startDate.getMonth()), dayEventRequest.getStartDate().getDayOfWeek(), occurrences);
        }
    }

    private int findDayOfMonthOccurrence(DayEventRequest dayEventRequest) {
        // For the given year/month and index returns the day of the month
        LocalDate firstDayOfMonth = LocalDate.of(dayEventRequest.getStartDate().getYear(), dayEventRequest.getStartDate().getMonth(), 1);
        int occurrences = 0;
        for (LocalDate date = firstDayOfMonth; !date.isAfter(dayEventRequest.getStartDate()); date = date.plusDays(1)) {
            if (date.getDayOfWeek().equals(dayEventRequest.getStartDate().getDayOfWeek())) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /*
        The date corresponds to the nth occurrence of a given day of the week within a month.
        2nd Tuesday of April 2024 -> returns the date 9/04/2024
     */
    private LocalDate findDateOfNthDayOfWeekInMonth(YearMonth yearMonth, DayOfWeek day, int occurrences) {
        LocalDate firstDayOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 1);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        LocalDate startDate = null;

        for (LocalDate date = firstDayOfMonth; !date.isAfter(lastDayOfMonth); date = date.plusDays(1)) {
            if (date.getDayOfWeek().equals(day)) {
                occurrences--;
            }
            if (occurrences == 0) {
                startDate = date;
                break;
            }
        }
        if (startDate == null) {
            logger.info("The {} of {} in {} is null", occurrences, day, yearMonth);
            throw new ServerErrorException("Internal Server Error");
        }
        return startDate;
    }

    private void createDayEventSlot(DayEventRequest dayEventRequest, DayEvent dayEvent, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate()));
        Set<String> guestEmails = null;
        if(dayEventRequest.getGuestEmails() != null) {
             guestEmails = dayEventRequest.getGuestEmails().stream()
                    .filter(guestEmail -> guestEmail.contains("@"))
                    .collect(Collectors.toSet());
        }
        DayEventSlot dayEventSlot = new DayEventSlot();
        dayEventSlot.setStartDate(startDate);
        dayEventSlot.setEndDate(endDate);
        dayEventSlot.setName(dayEventRequest.getName());
        dayEventSlot.setDescription(dayEventRequest.getDescription());
        dayEventSlot.setLocation(dayEventRequest.getLocation());
        dayEventSlot.setGuestEmails(guestEmails);
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
