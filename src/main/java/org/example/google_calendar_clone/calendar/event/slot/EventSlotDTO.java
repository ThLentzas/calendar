package org.example.google_calendar_clone.calendar.event.slot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class EventSlotDTO {
    protected UUID id;
    protected String name;
    protected String location;
    protected String description;
    protected String organizer;
    protected Set<String> guestEmails;
}
