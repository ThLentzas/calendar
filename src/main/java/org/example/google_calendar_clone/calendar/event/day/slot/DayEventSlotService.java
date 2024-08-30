package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.calendar.event.IEventSlotService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventSlotService implements IEventSlotService<DayEventRequest, DayEvent> {
    private final DayEventSlotRepository dayEventSlotRepository;

    @Override
    public void create(DayEventRequest dayEventRequest, DayEvent dayEvent) {
        switch (dayEventRequest.getRepetitionFrequency()) {
            case DAILY -> {
                /*
                    For every event we need to precompute the future day events as long as it repeats. In the DAILY case
                    we start from the start date of the event request and, we need to compute every event until the
                    repetition end date(when the repeating event should stop). As long as the start date has not reached
                    the repetition end date we create a day event slot, where the start date will be the current date and
                    the end date will be equal to the interval of the initial startDate - endDate. This is calculated
                    ChronoUnit.DAYS.between(dayEvent.getStartDate(), dayEvent.getEndDate()) explained below. Lastly,
                    we need to skip ahead equal to the number of the repetition step,(every 2 days/weeks/months etc)
                 */
                for(LocalDate date = dayEvent.getStartDate(); !date.isAfter(dayEvent.getRepetitionEndDate()); date = date.plusDays(dayEvent.getRepetitionStep())) {
                    DayEventSlot dayEventSlot = new DayEventSlot();
                    dayEventSlot.setStartDate(date);
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
                    dayEventSlot.setEndDate(date.plusDays(ChronoUnit.DAYS.between(dayEvent.getStartDate(), dayEvent.getEndDate())));
                    dayEventSlot.setName(dayEventRequest.getName());
                    dayEventSlot.setDescription(dayEventRequest.getDescription());
                    dayEventSlot.setGuestEmails(dayEventRequest.getGuestEmails());
                    dayEventSlot.setDayEvent(dayEvent);
                    this.dayEventSlotRepository.save(dayEventSlot);
                }
            }
        }
    }
}
