package org.example.google_calendar_clone.calendar.event.time.slot.dto;

import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.utils.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class TimeEventSlotDTOConverter implements Converter<TimeEventSlot, TimeEventSlotDTO> {

    @Override
    public TimeEventSlotDTO convert(TimeEventSlot timeEventSlot) {
        return TimeEventSlotDTO.builder()
                .id(timeEventSlot.getId())
                .name(timeEventSlot.getName())
                .startTime(DateUtils.convertFromUTC(timeEventSlot.getStartTime(), timeEventSlot.getStartTimeZoneId()))
                .endTime(DateUtils.convertFromUTC(timeEventSlot.getEndTime(), timeEventSlot.getEndTimeZoneId()))
                .startTimeZoneId(timeEventSlot.getStartTimeZoneId())
                .endTimeZoneId(timeEventSlot.getEndTimeZoneId())
                .location(timeEventSlot.getLocation())
                .description(timeEventSlot.getDescription())
                .organizer(timeEventSlot.getTimeEvent().getUser().getUsername())
                .guestEmails(timeEventSlot.getGuestEmails())
                .timeEventId(timeEventSlot.getTimeEvent().getId())
                .build();
    }
}
