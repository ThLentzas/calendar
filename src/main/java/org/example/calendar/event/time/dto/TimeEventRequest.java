package org.example.calendar.event.time.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.dto.AbstractEventRequest;
import org.example.calendar.event.groups.OnCreate;
import org.example.calendar.event.groups.OnUpdate;
import org.example.calendar.event.time.validator.ValidTimeEventCreateRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.validation.constraints.NotNull;

import org.example.calendar.event.time.validator.ValidTimeEventUpdateRequest;

/*
    We can not use @FutureOrPresent() on the date times because according to the annotation:
    Now is defined by the ClockProvider attached to the Validator or ValidatorFactory. The default clockProvider defines
    the current time according to the virtual machine, applying the current default time zone if needed. When compare
    the start time that the user provided to see if it is in the past, we can not just compare it with LocalDateTime.now()
    because it will use the default time zone. We need to make sure that their start time is in the future or present
    according to their timezone. Same logic needs to be followed for end time.

    What we do in the TimeEventRequestValidator is to convert the times to UTC based on the offset of their timezone and
    then compare it with ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
// Will include all fields
@EqualsAndHashCode(callSuper = true)
@ValidTimeEventCreateRequest(groups = OnCreate.class)
@ValidTimeEventUpdateRequest(groups = OnUpdate.class)
public class TimeEventRequest extends AbstractEventRequest {
    @NotNull(message = "The start time of the event is required. Please provide one", groups = OnCreate.class)
    // null will return true
    private LocalDateTime startTime;
    @NotNull(message = "The end time of the event is required. Please provide one", groups = OnCreate.class)
    private LocalDateTime endTime;
    // It throws ZoneRulesException: Unknown time-zone ID, for invalid timezone during deserialization
    @NotNull(message = "The time zone for the event's start time is required. Please provide one", groups = OnCreate.class)
    private ZoneId startTimeZoneId;
    @NotNull(message = "The time zone for the event's end time is required. Please provide one", groups = OnCreate.class)
    private ZoneId endTimeZoneId;
}
