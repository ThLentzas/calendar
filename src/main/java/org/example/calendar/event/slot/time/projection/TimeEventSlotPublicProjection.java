package org.example.calendar.event.slot.time.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.example.calendar.event.slot.projection.AbstractEventSlotPublicProjection;
import org.example.calendar.event.slot.projection.GuestProjection;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimeEventSlotPublicProjection extends AbstractEventSlotPublicProjection implements GuestProjection {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
}
