package org.example.google_calendar_clone.calendar.event;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.example.google_calendar_clone.calendar.event.day.dto.validator.OnCreate;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public abstract class EventRequest {
    @NotBlank(message = "Event name is required and cannot be blank. Please provide one", groups = OnCreate.class)
    protected String name;
    protected String location;
    protected String description;
    // use sql IN for email in emails
    protected Set<String> guestEmails;
    protected RepetitionFrequency repetitionFrequency;
    protected Integer repetitionStep;
    protected MonthlyRepetitionType monthlyRepetitionType;
    protected RepetitionDuration repetitionDuration;
    protected LocalDate repetitionEndDate;
    protected Integer repetitionCount;
}
