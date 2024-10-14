package org.example.calendar.event.slot.time.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.projection.AbstractEventSlotProjection;
import org.example.calendar.event.slot.projection.GuestProjection;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TimeEventSlotProjection extends AbstractEventSlotProjection implements GuestProjection {
    private LocalDateTime starTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
}
