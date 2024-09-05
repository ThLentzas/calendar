package org.example.google_calendar_clone.calendar.event.day.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.EventRequest;
import org.example.google_calendar_clone.calendar.event.day.dto.validator.OnCreate;
import org.example.google_calendar_clone.calendar.event.day.dto.validator.ValidDayEventRequest;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/*
    Our use case is the same with this. https://www.linkedin.com/pulse/validation-groups-spring-clearsolutionsltd-q7bvf

    "Consider a scenario where we have a UserDTO that is used in both POST (create user) and PATCH (update user)
    requests. In a POST request, all fields might be required, but in a PATCH request, only the fields to be updated are
    provided. How can we apply different validation rules for the same DTO based on the context?"
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
/*
    When @Validate() specifies a group like @Validate(groups = OnCreate.class) and we want our validator to be called
    with that group we need to pass the group name, otherwise it defaults and is not getting picked up.
    Default has to be specified, when we pass at least 1 argument, {OnCreate.class, Default.class}. If we don't specify
    anything it will default to Default.class
 */
@ValidDayEventRequest(groups = OnCreate.class)
@EqualsAndHashCode(callSuper = true)
public class DayEventRequest extends EventRequest {
    @NotNull(message = "The start date of the event is required. Please provide one", groups = OnCreate.class)
    // null will return true
    @FutureOrPresent(message = "The start date must be today or a future date", groups = OnCreate.class)
    private LocalDate startDate;
    @NotNull(message = "The end date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The end date must be today or a future date", groups = OnCreate.class)
    private LocalDate endDate;
}
