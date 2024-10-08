package org.example.calendar.event.slot.time.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.example.calendar.event.slot.AbstractEventSlotDTO;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeEventSlotDTO extends AbstractEventSlotDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
    /*
        We need the time event id to be part of the response dto because the user might request all TimeEventSlots for a
        given time event id. For example, user selected a time event slot that is part of a recurring time event. The user
        wants to see all the time event slots for that recurring time event. The client can easily get the day event id
        and make a GET request to "/api/v1/events/time-events/{eventId}" and it will return all the time event slots for
        that time event.
     */
    private UUID timeEventId;
}
