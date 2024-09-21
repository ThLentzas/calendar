package org.example.google_calendar_clone.calendar.event.day.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import org.example.google_calendar_clone.calendar.event.dto.AbstractEventRequest;
import org.example.google_calendar_clone.validator.day.ValidCreateDayEventRequest;
import org.example.google_calendar_clone.validator.day.ValidUpdateDayEventRequest;
import org.example.google_calendar_clone.validator.groups.OnCreate;
import org.example.google_calendar_clone.validator.groups.OnUpdate;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@ValidCreateDayEventRequest(groups = OnCreate.class)
@ValidUpdateDayEventRequest(groups = OnUpdate.class)
// Will include all fields
@EqualsAndHashCode(callSuper = true)
public class DayEventRequest extends AbstractEventRequest {
    // null will return true
    @NotNull(message = "The start date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The start date must be today or a future date", groups = {OnCreate.class, OnUpdate.class})
    private LocalDate startDate;
    @NotNull(message = "The end date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The end date must be today or a future date", groups = {OnCreate.class, OnUpdate.class})
    private LocalDate endDate;
}
