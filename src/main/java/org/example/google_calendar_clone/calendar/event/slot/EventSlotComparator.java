package org.example.google_calendar_clone.calendar.event.slot;

import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;

import java.util.Comparator;

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
