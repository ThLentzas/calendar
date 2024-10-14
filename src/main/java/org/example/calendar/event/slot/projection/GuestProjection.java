package org.example.calendar.event.slot.projection;

import java.util.Set;
import java.util.UUID;

public interface GuestProjection {
    UUID getId();
    Set<String> getGuestEmails();
}
