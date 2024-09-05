package org.example.google_calendar_clone.calendar.event;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public abstract class AbstractEventSlotDTO {
    protected UUID id;
    protected String name;
    protected String location;
    protected String description;
    protected Set<String> guestEmails;

    protected AbstractEventSlotDTO(UUID id, String name, String location, String description, Set<String> guestEmails) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.guestEmails = guestEmails;
    }
}
