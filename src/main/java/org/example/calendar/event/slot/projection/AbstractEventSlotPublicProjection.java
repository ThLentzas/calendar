package org.example.calendar.event.slot.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEventSlotPublicProjection {
    @EqualsAndHashCode.Include
    protected UUID id;
    protected String title;
    protected String location;
    protected String description;
    protected String organizer;
    protected Set<String> guestEmails;
    /*
        We need the day event id to be part of the response dto because the user might request all DayEventSlots for a
        given day event id. For example, user selected a day event slot that is part of a recurring day event. The user
        wants to see all the day event slots for that recurring day event. The client can easily get the day event id
        and make a GET request to "/api/v1/events/day-events/{eventId}" and it will return all the day event slots for
        that day event.
     */
    protected UUID eventId;
}
