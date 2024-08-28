package org.example.google_calendar_clone.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.example.google_calendar_clone.calendar.dto.validator.ValidDayEventRequest;
import org.example.google_calendar_clone.calendar.event.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.RepetitionFrequency;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ValidDayEventRequest
public class DayEventRequest {
    @NotBlank(message = "You must provide the name of the event")
    private String name;
    private String location;
    private String description;
    @NotBlank(message = "You must provide the starting date of the event")
    private LocalDate startDate;
    @NotBlank(message = "You must provide the ending date of the event")
    private LocalDate endDate;
    // use sql IN for email in emails
    private List<String> guestEmails;
    // How often is the Event repeated? (NEVER, DAILY, WEEKLY, MONTHLY, ANNUALLY)
    private RepetitionFrequency frequency;
    // For repeated events: what is the repetition step? (every two days/weeks)
    private Integer repetitionStep;
    // For monthly repeated events: which day of the month does it fall on? (same_day, same_weekday)
    private MonthlyRepetitionType monthlyRepetitionType;
    // For repeated events: for how long does the Event repeat? (forever, until_date, N_repetitions)
    private RepetitionDuration repetitionDuration;
    // For events repeated until a certain date: what is the date?
    private LocalDate repetitionEndDate;
    // For events repeated for a certain number of reps: how many reps?
    private Integer repetitionCount;
}
