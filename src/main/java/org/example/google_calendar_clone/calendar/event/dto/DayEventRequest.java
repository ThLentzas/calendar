package org.example.google_calendar_clone.calendar.event.dto;

import org.example.google_calendar_clone.calendar.event.dto.validator.OnCreate;
import org.example.google_calendar_clone.calendar.event.dto.validator.ValidDayEventRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/*
    Our use case is the same with this. https://www.linkedin.com/pulse/validation-groups-spring-clearsolutionsltd-q7bvf

    "Consider a scenario where we have a UserDTO that is used in both POST (create user) and PATCH (update user)
    requests. In a POST request, all fields might be required, but in a PATCH request, only the fields to be updated are
    provided. How can we apply different validation rules for the same DTO based on the context?"
 */
@Getter
@Setter
/*
    When @Validate() specifies a group like @Validate(groups = OnCreate.class) and we want our validator to be called
    with that group we need to pass the group name, otherwise it defaults and is not getting picked up.
    Default has to be specified, when we pass at least 1 argument, {OnCreate.class, Default.class}. If we don't specify
    anything it will default to Default.class
 */
@ValidDayEventRequest(groups = OnCreate.class)
public class DayEventRequest {
    @NotBlank(message = "Event name is required and cannot be blank. Please provide one", groups = OnCreate.class)
    private String name;
    private String location;
    private String description;
    @NotNull(message = "The start date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The start date must be today or a future date", groups = OnCreate.class)
    private LocalDate startDate;
    @NotNull(message = "The end date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The end date must be today or a future date", groups = OnCreate.class)
    private LocalDate endDate;
    // use sql IN for email in emails
    private List<String> guestEmails;
    // How often is the Event repeated?
    private RepetitionFrequency repetitionFrequency;
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
