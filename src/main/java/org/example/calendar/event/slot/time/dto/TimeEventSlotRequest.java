package org.example.calendar.event.slot.time.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.AbstractEventSlotRequest;
import org.example.calendar.event.slot.time.validator.ValidTimeEventSlotRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ValidTimeEventSlotRequest
@EqualsAndHashCode(callSuper = true)
public class TimeEventSlotRequest extends AbstractEventSlotRequest {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // It throws ZoneRulesException: Unknown time-zone ID, for invalid timezone during deserialization
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
}
