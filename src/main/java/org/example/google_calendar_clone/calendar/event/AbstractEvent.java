package org.example.google_calendar_clone.calendar.event;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.entity.User;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.UUID;

/*
    We can not create a class Repetition and have all the related properties there because Hibernate will see it as an
    object and, it will require some sort of relationship()
 */
@Getter
@Setter
@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    protected UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(updatable = false) // Can't use @Column for associations
    protected User user;
    // How often is the Event repeated?(never, daily, weekly, monthly, annually)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    protected RepetitionFrequency repetitionFrequency;
    // For repeated events: what is the repetition step? (every two days/weeks)
    protected Integer repetitionStep;
    // For monthly repeated events: which day of the month does it fall on? (same_day, same_weekday)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    protected MonthlyRepetitionType monthlyRepetitionType;
    // For repeated events: for how long does the Event repeat? (forever, until_date, N_repetitions)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    protected RepetitionDuration repetitionDuration;
    // For events repeated until a certain date: what is the date?
    protected LocalDate repetitionEndDate;
    // For events repeated for a certain number of reps: how many reps?
    protected Integer repetitionCount;
    // For events repeated for a certain number of reps: what is the current count?
    // protected Integer currentRepetition; !!!! We don't care about it, we are going to compute all repetitionCount events
}

