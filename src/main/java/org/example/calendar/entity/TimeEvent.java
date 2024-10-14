package org.example.calendar.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;

import org.example.calendar.event.AbstractEvent;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TimeEvent extends AbstractEvent {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;

    // deep copy, copy constructor
    public TimeEvent(TimeEvent original) {
        this.id = original.id;  // UUID is immutable, safe to copy
        this.organizerId = original.organizerId;  // Long is immutable
        this.recurrenceFrequency = original.recurrenceFrequency;  // Enum, immutable
        this.recurrenceStep = original.recurrenceStep;  // Integer is immutable
        this.weeklyRecurrenceDays = original.weeklyRecurrenceDays == null ? null : EnumSet.copyOf(new HashSet<>(original.weeklyRecurrenceDays));
        this.monthlyRecurrenceType = original.monthlyRecurrenceType;  // Enum, immutable
        this.recurrenceDuration = original.recurrenceDuration;  // Enum, immutable
        this.recurrenceEndDate = original.recurrenceEndDate;  // LocalDate is immutable
        this.numberOfOccurrences = original.numberOfOccurrences;  // Integer is immutable
        this.startTime = original.startTime; // LocalDateTime is immutable
        this.startTimeZoneId = original.getStartTimeZoneId(); // ZoneId is immutable
        this.endTime = original.endTime; // LocalDateTime is immutable
        this.endTimeZoneId = original.getEndTimeZoneId(); // ZoneId is immutable
    }
}
