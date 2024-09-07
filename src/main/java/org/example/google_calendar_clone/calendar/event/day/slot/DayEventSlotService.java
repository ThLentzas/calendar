package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.calendar.event.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTOConverter;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.utils.DateUtils;
import org.springframework.stereotype.Service;

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
    private static final DayEventSlotDTOConverter converter = new DayEventSlotDTOConverter();

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
        // We want to send invitation emails only to emails that at least contain @
        Set<String> guestEmails = new HashSet<>();
        if (dayEventRequest.getGuestEmails() != null) {
            guestEmails = dayEventRequest.getGuestEmails().stream()
                    .filter(guestEmail -> guestEmail.contains("@"))
                    .collect(Collectors.toSet());
        }
        dayEventRequest.setGuestEmails(guestEmails);

        switch (dayEventRequest.getRepetitionFrequency()) {
            // Since the event is not to be repeated, we only create 1 TimeEventSlot
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
                .map(converter::convert)
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

        int occurrences = DateUtils.findDayOfMonthOccurrence(dayEventRequest.getStartDate());
        LocalDate startDate;
        for (LocalDate date = dayEventRequest.getStartDate(); !date.isAfter(dayEventRequest.getRepetitionEndDate()); date = date.plusMonths(dayEventRequest.getRepetitionStep())) {
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(YearMonth.of(date.getYear(), date.getMonth()), dayEventRequest.getStartDate().getDayOfWeek(), occurrences);
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
        }
    }

    // Monthly events that repeat the same week day until a number of repetitions(2nd Tuesday of the month)
    private void createNRepetitionsMonthlySameWeekDayEventSlots(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        if (dayEventRequest.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }
        int occurrences = DateUtils.findDayOfMonthOccurrence(dayEventRequest.getStartDate());
        LocalDate startDate = dayEventRequest.getStartDate();
        for (int i = 0; i <= dayEventRequest.getRepetitionCount(); i++) {
            createDayEventSlot(dayEventRequest, dayEvent, startDate);
            startDate = startDate.plusMonths(dayEventRequest.getRepetitionStep());
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(YearMonth.of(startDate.getYear(), startDate.getMonth()), dayEventRequest.getStartDate().getDayOfWeek(), occurrences);
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
