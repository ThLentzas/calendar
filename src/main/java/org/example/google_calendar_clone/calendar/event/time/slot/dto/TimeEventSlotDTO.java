package org.example.google_calendar_clone.calendar.event.time.slot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.example.google_calendar_clone.calendar.event.EventSlotDTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TimeEventSlotDTO extends EventSlotDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
    /*
        We need the time event id to be part of the response dto because the user might request all TimeEventSlots for a
        given time event id. For example, user selected a time event slot that is part of a repeating time event. The user
        wants to see all the time event slots for that repeating time event. The client can easily get the day event id
        and make a GET request to "/api/v1/events/time-events/{eventId}" and it will return all the time event slots for
        that time event.
     */
    private UUID timeEventId;
}
