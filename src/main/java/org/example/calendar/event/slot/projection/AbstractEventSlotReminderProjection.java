package org.example.calendar.event.slot.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

import org.example.calendar.entity.User;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractEventSlotReminderProjection {
    protected UUID id;
    protected String title;
    protected String organizerUsername;
    protected String organizerEmail;
    protected Set<String> guestEmails;
}
