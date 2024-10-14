package org.example.calendar.event.slot;

import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.slot.projection.AbstractEventSlotPublicProjection;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;

import java.util.Comparator;
/*
    Both the DayEventSlots and TimeEventSlots are sorted but when we add them in 1 list, we need to make sure
    that they are also sorted based on their starting date. We need a comparator so that we can compare the
    starting date of DayEventSlot with the starting dateTime of the TimeEventSlot. If we have 2 DayEventSlots
    we compare their starting date, if we have 2 TimeEventSlots we compare their starting time.
 */
public class EventSlotComparator implements Comparator<AbstractEventSlotPublicProjection> {

    @Override
    public int compare(AbstractEventSlotPublicProjection o1, AbstractEventSlotPublicProjection o2) {
        if (o1 instanceof DayEventSlotPublicProjection dayEventSlot1) {
            if (o2 instanceof DayEventSlotPublicProjection dayEventSlot2) {
                // Compare by startDate
                return dayEventSlot1.getStartDate().compareTo(dayEventSlot2.getStartDate());
            } else if (o2 instanceof TimeEventSlotPublicProjection timeEventSlot) {
                // Compare startDate of DayEventSlotDTO with startTime of TimeEventSlotDTO
                return dayEventSlot1.getStartDate().atStartOfDay().compareTo(timeEventSlot.getStartTime());
            }
        } else if (o1 instanceof TimeEventSlotPublicProjection timeEventSlot1) {
            // Compare by startTime
            if (o2 instanceof TimeEventSlotPublicProjection timeEventSlot2) {
                return timeEventSlot1.getStartTime().compareTo(timeEventSlot2.getStartTime());
            } else if (o2 instanceof DayEventSlotPublicProjection dayEventSlot) {
                // Compare startTime of TimeEventSlotDTO with startDate of DayEventSlotDTO
                return timeEventSlot1.getStartTime().compareTo(dayEventSlot.getStartDate().atStartOfDay());
            }
        }
        return 0;
    }
}
