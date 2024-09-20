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

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@ValidCreateDayEventRequest
// Will include all fields
@EqualsAndHashCode(callSuper = true)
public class CreateDayEventRequest extends AbstractEventRequest {
    @NotNull(message = "The start date of the event is required. Please provide one")
    // null will return true
    @FutureOrPresent(message = "The start date must be today or a future date")
    private LocalDate startDate;
    @NotNull(message = "The end date of the event is required. Please provide one")
    @FutureOrPresent(message = "The end date must be today or a future date")
    private LocalDate endDate;
}
