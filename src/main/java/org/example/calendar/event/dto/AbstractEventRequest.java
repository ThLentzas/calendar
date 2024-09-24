package org.example.calendar.event.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public abstract class AbstractEventRequest {
    protected String title;
    protected String location;
    protected String description;
    protected Set<String> guestEmails;
    protected RecurrenceFrequency recurrenceFrequency;
    protected Integer recurrenceStep;
    protected EnumSet<DayOfWeek> weeklyRecurrenceDays;
    protected MonthlyRecurrenceType monthlyRecurrenceType;
    protected RecurrenceDuration recurrenceDuration;
    protected LocalDate recurrenceEndDate;
    protected Integer numberOfOccurrences;
}
