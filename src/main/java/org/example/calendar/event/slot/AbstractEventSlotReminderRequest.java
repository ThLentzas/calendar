package org.example.calendar.event.slot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.example.calendar.entity.User;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractEventSlotReminderRequest {
    protected UUID id;
    protected String title;
    protected User organizer;
    protected Set<String> guestEmails;
}
