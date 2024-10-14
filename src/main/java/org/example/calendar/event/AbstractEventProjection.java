package org.example.calendar.event;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEventProjection {
    @EqualsAndHashCode.Include
    protected UUID id;
    // @ManyToOne. A user can be an organizer in many events. Every event has only one organizer
    protected Long organizerId;
    // How often is the Event occurs?(never, daily, weekly, monthly, annually)
    protected RecurrenceFrequency recurrenceFrequency;
    // For recurring events: what is the recurrence step? (every two days/weeks)
    protected Integer recurrenceStep;
    // For weekly recurring events: which days of the week does it fall on?
    protected Set<DayOfWeek> weeklyRecurrenceDays;
    // For monthly recurring events: which day of the month does it fall on? (same_day, same_weekday)
    protected MonthlyRecurrenceType monthlyRecurrenceType;
    // For recurring events: for how long does the Event recur? (forever, until_date, N_Occurrences)
    protected RecurrenceDuration recurrenceDuration;
    // For events recurring until a certain date: what is the date?
    protected LocalDate recurrenceEndDate;
    // For events recurring for a certain number of reps: how many reps?
    protected Integer numberOfOccurrences;
}
