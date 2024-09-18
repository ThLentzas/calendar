package org.example.google_calendar_clone.calendar.event.slot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractEventSlotReminderEmailRequest {
    protected String eventName;
    protected String location;
    protected String organizer;
    protected String description;
    protected Set<String> guestEmails;
}
