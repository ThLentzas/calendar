package org.example.calendar.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractEventInvitationRequest {
    protected String eventName;
    protected String organizer;
    protected String location;
    protected String description;
    protected Set<String> guestEmails;
    protected RecurrenceFrequency recurrenceFrequency;
    protected Integer recurrenceStep;
    protected EnumSet<DayOfWeek> weeklyRecurrenceDays;
    protected MonthlyRecurrenceType monthlyRecurrenceType;
    protected RecurrenceDuration recurrenceDuration;
    protected LocalDate recurrenceEndDate;
    protected Integer numbersOfOccurrences;
}
