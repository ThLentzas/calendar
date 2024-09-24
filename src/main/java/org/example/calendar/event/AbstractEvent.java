package org.example.calendar.event;

import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.dto.WeeklyRecurrenceDaysConverter;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.entity.User;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

/*
    We can not create a class Recurrence and have all the related properties there because Hibernate will see it as an
    object and, it will require some sort of relationship
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
public abstract class AbstractEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    protected UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(updatable = false) // Can't use @Column for associations
    protected User user;
    // How often is the Event occurs?(never, daily, weekly, monthly, annually)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    protected RecurrenceFrequency recurrenceFrequency;
    // For recurring events: what is the recurrence step? (every two days/weeks)
    protected Integer recurrenceStep;
    // For weekly recurring events: which days of the week does it fall on?
    @Convert(converter = WeeklyRecurrenceDaysConverter.class)
    protected EnumSet<DayOfWeek> weeklyRecurrenceDays;
    // For monthly recurring events: which day of the month does it fall on? (same_day, same_weekday)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    protected MonthlyRecurrenceType monthlyRecurrenceType;
    // For recurring events: for how long does the Event recur? (forever, until_date, N_Occurrences)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    protected RecurrenceDuration recurrenceDuration;
    // For events recurring until a certain date: what is the date?
    protected LocalDate recurrenceEndDate;
    // For events recurring for a certain number of reps: how many reps?
    protected Integer numberOfOccurrences;
}

