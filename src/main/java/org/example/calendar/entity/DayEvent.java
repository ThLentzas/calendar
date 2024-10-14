package org.example.calendar.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;

import org.example.calendar.event.AbstractEvent;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DayEvent extends AbstractEvent {
    private LocalDate startDate;
    private LocalDate endDate;

    // Copy constructor, deep copy
    public DayEvent(DayEvent original) {
        super();
        this.id = original.id;  // UUID is immutable, safe to copy
        this.organizerId = original.organizerId;  // Long is immutable
        this.recurrenceFrequency = original.recurrenceFrequency;  // Enum, immutable
        this.recurrenceStep = original.recurrenceStep;  // Integer is immutable
        this.weeklyRecurrenceDays = original.weeklyRecurrenceDays == null ? null : EnumSet.copyOf(new HashSet<>(original.weeklyRecurrenceDays));
        this.monthlyRecurrenceType = original.monthlyRecurrenceType;  // Enum, immutable
        this.recurrenceDuration = original.recurrenceDuration;  // Enum, immutable
        this.recurrenceEndDate = original.recurrenceEndDate;  // LocalDate is immutable
        this.numberOfOccurrences = original.numberOfOccurrences;  // Integer is immutable
        this.startDate = original.startDate;  // LocalDate is immutable
        this.endDate = original.endDate;  // LocalDate is immutable
    }
}
