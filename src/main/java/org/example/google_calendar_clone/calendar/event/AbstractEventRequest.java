package org.example.google_calendar_clone.calendar.event;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.day.dto.validator.OnCreate;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode
public abstract class AbstractEventRequest {
    @NotBlank(message = "Event name is required and cannot be blank. Please provide one", groups = OnCreate.class)
    protected String name;
    protected String location;
    protected String description;
    protected Set<String> guestEmails;
    protected RepetitionFrequency repetitionFrequency;
    protected Integer repetitionStep;
    protected MonthlyRepetitionType monthlyRepetitionType;
    protected RepetitionDuration repetitionDuration;
    protected LocalDate repetitionEndDate;
    protected Integer repetitionCount;
}
