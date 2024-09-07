package org.example.google_calendar_clone.calendar.event.time.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.AbstractEventRequest;
import org.example.google_calendar_clone.calendar.event.day.dto.validator.OnCreate;
import org.example.google_calendar_clone.calendar.event.time.dto.validator.ValidTimeEventRequest;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@ValidTimeEventRequest(groups = OnCreate.class)
@EqualsAndHashCode(callSuper = true)
public class TimeEventRequest extends AbstractEventRequest {
    @NotNull(message = "The start date of the event is required. Please provide one", groups = OnCreate.class)
    // null will return true
    @FutureOrPresent(message = "The start date must be today or a future date", groups = OnCreate.class)
    private LocalDateTime startTime;
    @NotNull(message = "The end date of the event is required. Please provide one", groups = OnCreate.class)
    @FutureOrPresent(message = "The end date must be today or a future date", groups = OnCreate.class)
    private LocalDateTime endTime;
    // It throws ZoneRulesException: Unknown time-zone ID, for invalid timezone during deserialization
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
}
