package org.example.google_calendar_clone.calendar.event.time.slot;

import org.example.google_calendar_clone.calendar.event.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
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

        switch (eventRequest.getRepetitionFrequency()) {
            case NEVER -> createTimeEventSlot(eventRequest, event, eventRequest.getStartTime());
            case DAILY -> {
                createUntilDateEventSlots(eventRequest, event, ChronoUnit.DAYS);
                createNRepetitionsEventSlots(eventRequest, event, ChronoUnit.DAYS);
            }
            case WEEKLY -> {
                createUntilDateEventSlots(eventRequest, event, ChronoUnit.WEEKS);
                createNRepetitionsEventSlots(eventRequest, event, ChronoUnit.WEEKS);
            }
            case MONTHLY -> {
                if (eventRequest.getMonthlyRepetitionType().equals(MonthlyRepetitionType.SAME_WEEKDAY)) {
                    createUntilDateMonthlySameWeekDayEventSlots(eventRequest, event);
                    createNRepetitionsMonthlySameWeekDayEventSlots(eventRequest, event);
                } else {
                    createUntilDateEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                    createNRepetitionsEventSlots(eventRequest, event, ChronoUnit.MONTHS);
                }
            }
            case ANNUALLY -> {
                createUntilDateEventSlots(eventRequest, event, ChronoUnit.YEARS);
                createNRepetitionsEventSlots(eventRequest, event, ChronoUnit.YEARS);
            }
        }
    }

    @Override
    public List<TimeEventSlotDTO> findEventSlotsByEventId(UUID eventId) {
        return List.of();
    }

    /*
        In the plus() method we can pass a unit of time to increase our date in the loop. According to the unit passed
        plusWeeks(), plusDays() etc will be called. It is a way to avoid having a different case for each value

        date = date.plusDays(), date = date.plusWeeks() etc
     */
    private void createUntilDateEventSlots(TimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (eventRequest.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && event.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        for (LocalDateTime dateTime = event.getStartTime(); !dateTime.toLocalDate().isAfter(event.getRepetitionEndDate());
             dateTime = dateTime.plus(event.getRepetitionStep(), unit)) {
            createTimeEventSlot(eventRequest, event, dateTime);
        }
    }

    private void createNRepetitionsEventSlots(TimeEventRequest eventRequest, TimeEvent event, ChronoUnit unit) {
        if (eventRequest.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }

        LocalDateTime startTime = eventRequest.getStartTime();
        for (int i = 0; i <= eventRequest.getRepetitionCount(); i++) {
            createTimeEventSlot(eventRequest, event, startTime);
           /*
                The starDate is updated to the previous value plus the number of the repetition step which
                can be 1, 2 etc, meaning the event is to be repeated every 1,2, days until we reach
                N_REPETITIONS
            */
            startTime = startTime.plus(eventRequest.getRepetitionStep(), unit);
        }
    }

    // Monthly events that repeat the same week day until a certain date(2nd Tuesday of the month)
    private void createUntilDateMonthlySameWeekDayEventSlots(TimeEventRequest eventRequest, TimeEvent event) {
        if (eventRequest.getRepetitionDuration() != RepetitionDuration.UNTIL_DATE
                && eventRequest.getRepetitionDuration() != RepetitionDuration.FOREVER) {
            return;
        }

        int occurrences = DateUtils.findDayOfMonthOccurrence(LocalDate.from(eventRequest.getStartTime()));
        LocalDate startDate;
        LocalDateTime startTime;
        for (LocalDateTime dateTime = event.getStartTime(); !dateTime.toLocalDate().isAfter(eventRequest.getRepetitionEndDate()); dateTime = dateTime.plusMonths(eventRequest.getRepetitionStep())) {
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(YearMonth.of(dateTime.getYear(), dateTime.getMonth()), eventRequest.getStartTime().getDayOfWeek(), occurrences);
            // Combines this date with a time to create a LocalDateTime.
            // Returns LocalDateTime formed from this date at the specified time.
            startTime = startDate.atTime(eventRequest.getStartTime().toLocalTime());
            createTimeEventSlot(eventRequest, event, startTime);
        }
    }

    // Monthly events that repeat the same week day until a number of repetitions(2nd Tuesday of the month)
    private void createNRepetitionsMonthlySameWeekDayEventSlots(TimeEventRequest eventRequest, TimeEvent event) {
        if (eventRequest.getRepetitionDuration() != RepetitionDuration.N_REPETITIONS) {
            return;
        }
        int occurrences = DateUtils.findDayOfMonthOccurrence(LocalDate.from(eventRequest.getStartTime()));
        LocalDate startDate = LocalDate.from(eventRequest.getStartTime());
        LocalDateTime startTime = eventRequest.getStartTime();
        for (int i = 0; i <= eventRequest.getRepetitionCount(); i++) {
            createTimeEventSlot(eventRequest, event, startTime);
            startDate = LocalDate.from(startDate).plusMonths(eventRequest.getRepetitionStep());
            startDate = DateUtils.findDateOfNthDayOfWeekInMonth(YearMonth.of(startDate.getYear(), startDate.getMonth()), eventRequest.getStartTime().getDayOfWeek(), occurrences);
            // We want the upcoming events to start the same time as the original one. We find the next date and add the time
            startTime = startDate.atTime(eventRequest.getStartTime().toLocalTime());
        }
    }

    /*
        The user provides the start time and end time to their preferred timezone. We are storing the start time and
        end time to UTC. When we will display the event to the user, we can convert back to the user's timezone that we
        stored along the start and end time. This way we have consistency in the times stored in our db and we can adjust
        accordingly.
     */
    private void createTimeEventSlot(TimeEventRequest eventRequest, TimeEvent event, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plusDays(getEventDuration(event.getStartTime(), eventRequest.getEndTime()));
        startTime = DateUtils.convertToUTC(startTime, eventRequest.getStartTimeZoneId());
        endTime = DateUtils.convertToUTC(endTime, eventRequest.getEndTimeZoneId());

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
        return (int) ChronoUnit.DAYS.between(startTime, endTime);
    }
}
