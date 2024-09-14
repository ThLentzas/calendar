package org.example.google_calendar_clone.calendar.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractEventInvitationEmailRequest {
    protected String eventName;
    protected String location;
    protected String organizer;
    protected Set<String> guestEmails;
    protected RepetitionFrequency repetitionFrequency;
    protected Integer repetitionStep;
    protected EnumSet<DayOfWeek> weeklyRecurrenceDays;
    protected MonthlyRepetitionType monthlyRepetitionType;
    protected RepetitionDuration repetitionDuration;
    protected LocalDate repetitionEndDate;
    protected Integer repetitionOccurrences;
}
