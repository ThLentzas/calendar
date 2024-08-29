package org.example.google_calendar_clone.calendar;

import jakarta.persistence.*;

import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.entity.User;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractCalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
    protected String name;
    protected String location;
    protected String description;
    @ManyToMany(fetch = FetchType.LAZY)
    protected Set<User> guests;
    @ManyToOne(fetch = FetchType.LAZY)
    protected User host;
    // How often is the Event repeated?
    protected RepetitionFrequency frequency;
    // For repeated events: what is the repetition step? (every two days/weeks)
    protected Integer repetitionStep;
    // For monthly repeated events: which day of the month does it fall on? (same_day, same_weekday)
    protected MonthlyRepetitionType monthlyRepetitionType;
    // For repeated events: for how long does the Event repeat? (forever, until_date, N_repetitions)
    protected RepetitionDuration repetitionDuration;
    // For events repeated until a certain date: what is the date?
    protected LocalDate repetitionEndDate;
    // For events repeated for a certain number of reps: how many reps?
    protected Integer repetitionCount;
    // For events repeated for a certain number of reps: what is the current count?
    protected Integer currentRepetition;
}

