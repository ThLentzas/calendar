package org.example.calendar.event.slot.day.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

import org.example.calendar.event.slot.AbstractEventSlotDTO;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DayEventSlotDTO extends AbstractEventSlotDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    /*
        We need the day event id to be part of the response dto because the user might request all DayEventSlots for a
        given day event id. For example, user selected a day event slot that is part of a recurring day event. The user
        wants to see all the day event slots for that recurring day event. The client can easily get the day event id
        and make a GET request to "/api/v1/events/day-events/{eventId}" and it will return all the day event slots for
        that day event.
     */
    private UUID dayEventId;
}
