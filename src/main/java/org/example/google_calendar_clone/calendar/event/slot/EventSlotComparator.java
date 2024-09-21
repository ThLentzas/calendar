package org.example.google_calendar_clone.calendar.event.slot;

import org.example.google_calendar_clone.calendar.event.slot.day.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.slot.time.dto.TimeEventSlotDTO;

import java.util.Comparator;
/*
    Both the DayEventSlots and TimeEventSlots are sorted but when we add them in 1 list, we need to make sure
    that they are also sorted based on their starting date. We need a comparator so that we can compare the
    starting date of DayEventSlot with the starting dateTime of the TimeEventSlot. If we have 2 DayEventSlots
    we compare their starting date, if we have 2 TimeEventSlots we compare their starting time.
 */
public class EventSlotComparator implements Comparator<EventSlotDTO> {

    @Override
    public int compare(EventSlotDTO o1, EventSlotDTO o2) {
        if (o1 instanceof DayEventSlotDTO dayEventSlot1) {
            if (o2 instanceof DayEventSlotDTO dayEventSlot2) {
                // Compare by startDate
                return dayEventSlot1.getStartDate().compareTo(dayEventSlot2.getStartDate());
            } else if (o2 instanceof TimeEventSlotDTO timeEventSlot) {
                // Compare startDate of DayEventSlotDTO with startTime of TimeEventSlotDTO
                return dayEventSlot1.getStartDate().atStartOfDay().compareTo(timeEventSlot.getStartTime());
            }
        } else if (o1 instanceof TimeEventSlotDTO timeEventSlot1) {
            // Compare by startTime
            if (o2 instanceof TimeEventSlotDTO timeEventSlot2) {
                return timeEventSlot1.getStartTime().compareTo(timeEventSlot2.getStartTime());
            } else if (o2 instanceof DayEventSlotDTO dayEventSlot) {
                // Compare startTime of TimeEventSlotDTO with startDate of DayEventSlotDTO
                return timeEventSlot1.getStartTime().compareTo(dayEventSlot.getStartDate().atStartOfDay());
            }
        }
        return 0;
    }
}
