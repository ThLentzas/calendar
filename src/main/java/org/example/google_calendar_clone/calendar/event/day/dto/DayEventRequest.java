package org.example.google_calendar_clone.calendar.event.day.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.dto.AbstractEventRequest;
import org.example.google_calendar_clone.calendar.event.day.validator.ValidDayEventCreateRequest;
import org.example.google_calendar_clone.calendar.event.day.validator.ValidDayEventUpdateRequest;
import org.example.google_calendar_clone.calendar.event.groups.OnCreate;
import org.example.google_calendar_clone.calendar.event.groups.OnUpdate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
// Will include all fields
@EqualsAndHashCode(callSuper = true)
@ValidDayEventCreateRequest(groups = OnCreate.class)
@ValidDayEventUpdateRequest(groups = OnUpdate.class)
public class DayEventRequest extends AbstractEventRequest {
    // null will return true
    @NotNull(message = "The start date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The start date must be today or a future date", groups = {OnCreate.class, OnUpdate.class})
    private LocalDate startDate;
    @NotNull(message = "The end date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The end date must be today or a future date", groups = {OnCreate.class, OnUpdate.class})
    private LocalDate endDate;
}
