package org.example.google_calendar_clone.calendar.event.day.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.dto.AbstractEventRequest;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import org.example.google_calendar_clone.validator.day.ValidUpdateDayEventRequest;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@ValidUpdateDayEventRequest
// Will include all fields
@EqualsAndHashCode(callSuper = true)
public class UpdateDayEventRequest extends AbstractEventRequest {
    // null will return true
    @FutureOrPresent(message = "The start date must be today or a future date")
    private LocalDate startDate;
    @FutureOrPresent(message = "The end date must be today or a future date")
    private LocalDate endDate;
}
