package org.example.calendar.event.time.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.example.calendar.event.AbstractEventProjection;
import org.example.calendar.event.slot.time.projection.TimeEventSlotProjection;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TimeEventProjection extends AbstractEventProjection {
    private LocalDateTime starTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
    private List<TimeEventSlotProjection> eventSlots;
}
