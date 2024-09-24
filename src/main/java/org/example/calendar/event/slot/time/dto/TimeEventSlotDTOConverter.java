package org.example.calendar.event.slot.time.dto;

import org.example.calendar.entity.TimeEventSlot;
import org.example.calendar.utils.DateUtils;
import org.springframework.core.convert.converter.Converter;

import java.util.TreeSet;

public class TimeEventSlotDTOConverter implements Converter<TimeEventSlot, TimeEventSlotDTO> {

    @Override
    public TimeEventSlotDTO convert(TimeEventSlot timeEventSlot) {
        return TimeEventSlotDTO.builder()
                .id(timeEventSlot.getId())
                .title(timeEventSlot.getTitle())
                .startTime(DateUtils.convertFromUTC(timeEventSlot.getStartTime(), timeEventSlot.getStartTimeZoneId()))
                .endTime(DateUtils.convertFromUTC(timeEventSlot.getEndTime(), timeEventSlot.getEndTimeZoneId()))
                .startTimeZoneId(timeEventSlot.getStartTimeZoneId())
                .endTimeZoneId(timeEventSlot.getEndTimeZoneId())
                .location(timeEventSlot.getLocation())
                .description(timeEventSlot.getDescription())
                .organizer(timeEventSlot.getTimeEvent().getUser().getUsername())
                .guestEmails(new TreeSet<>(timeEventSlot.getGuestEmails()))
                .timeEventId(timeEventSlot.getTimeEvent().getId())
                .build();
    }
}
