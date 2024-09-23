package org.example.google_calendar_clone.calendar.event.slot.day.dto;

import org.example.google_calendar_clone.entity.DayEventSlot;
import org.springframework.core.convert.converter.Converter;

import java.util.TreeSet;

public class DayEventSlotDTOConverter implements Converter<DayEventSlot, DayEventSlotDTO> {

    @Override
    public DayEventSlotDTO convert(DayEventSlot dayEventSlot) {
        return DayEventSlotDTO.builder()
                .id(dayEventSlot.getId())
                .title(dayEventSlot.getTitle())
                .startDate(dayEventSlot.getStartDate())
                .endDate(dayEventSlot.getEndDate())
                .location(dayEventSlot.getLocation())
                .description(dayEventSlot.getDescription())
                .organizer(dayEventSlot.getDayEvent().getUser().getUsername())
                .guestEmails(new TreeSet<>(dayEventSlot.getGuestEmails()))
                .dayEventId(dayEventSlot.getDayEvent().getId())
                .build();
    }
}
