package org.example.calendar.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import org.example.calendar.event.slot.AbstractEventSlot;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TimeEventSlot extends AbstractEventSlot {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
    private Set<String> guestEmails;

    // deep copy, copy constructor
    public TimeEventSlot(TimeEventSlot original) {
        this.id = original.id;  // UUID is immutable, no need for deep copy
        this.eventId = original.eventId;  // UUID is immutable
        this.title = original.title;  // String is immutable
        this.location = original.location;  // String is immutable
        this.description = original.description;  // String is immutable
        this.startTime = original.startTime;  // LocalDateTime is immutable
        this.endTime = original.endTime;  // LocalDateTime is immutable
        this.startTimeZoneId = original.startTimeZoneId;  // ZoneId is immutable
        this.endTimeZoneId = original.endTimeZoneId;  // ZoneId is immutable
        // Deep copy of the mutable Set (guestEmails)
        this.guestEmails = original.guestEmails == null ? null : new HashSet<>(original.guestEmails);
    }
}
