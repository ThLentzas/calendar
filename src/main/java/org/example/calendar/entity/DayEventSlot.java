package org.example.calendar.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.example.calendar.event.slot.AbstractEventSlot;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DayEventSlot extends AbstractEventSlot {
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<String> guestEmails;

    // deep copy, copy constructor
    public DayEventSlot(DayEventSlot original) {
        this.id = original.id;  // UUID is immutable, so no need for deep copy
        this.eventId = original.eventId;  // UUID is immutable
        this.title = original.title;  // String is immutable
        this.location = original.location;  // String is immutable
        this.description = original.description;  // String is immutable
        this.startDate = original.startDate;  // LocalDate is immutable
        this.endDate = original.endDate;  // LocalDate is immutable
        // Deep copy of the mutable Set (guestEmails)
        this.guestEmails = original.guestEmails == null ? null :  new HashSet<>(original.guestEmails);
    }
}
