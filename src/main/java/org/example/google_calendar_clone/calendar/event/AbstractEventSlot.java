package org.example.google_calendar_clone.calendar.event;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEventSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
    protected String name;
    protected String location;
    protected String description;
    @ElementCollection
    protected Set<String> guestEmails;
}
