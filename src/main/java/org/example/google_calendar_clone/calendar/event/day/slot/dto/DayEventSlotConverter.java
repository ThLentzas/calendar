package org.example.google_calendar_clone.calendar.event.day.slot.dto;

import org.example.google_calendar_clone.entity.DayEventSlot;
import org.springframework.core.convert.converter.Converter;

public class DayEventSlotConverter implements Converter<DayEventSlot, DayEventSlotDTO> {

    @Override
    public DayEventSlotDTO convert(DayEventSlot dayEventSlot) {
        return new DayEventSlotDTO(
                dayEventSlot.getId(),
                dayEventSlot.getName(),
                dayEventSlot.getStartDate(),
                dayEventSlot.getEndDate(),
                dayEventSlot.getLocation(),
                dayEventSlot.getDescription(),
                dayEventSlot.getGuestEmails()
        );
    }
}
