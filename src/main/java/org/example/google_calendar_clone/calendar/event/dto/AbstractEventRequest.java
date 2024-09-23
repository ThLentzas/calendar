package org.example.google_calendar_clone.calendar.event.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

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
    protected RepetitionFrequency repetitionFrequency;
    protected Integer repetitionStep;
    protected EnumSet<DayOfWeek> weeklyRecurrenceDays;
    protected MonthlyRepetitionType monthlyRepetitionType;
    protected RepetitionDuration repetitionDuration;
    protected LocalDate repetitionEndDate;
    protected Integer repetitionOccurrences;
}
