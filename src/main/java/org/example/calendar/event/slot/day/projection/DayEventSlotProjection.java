package org.example.calendar.event.slot.day.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.projection.AbstractEventSlotProjection;
import org.example.calendar.event.slot.projection.GuestProjection;

import java.time.LocalDate;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DayEventSlotProjection extends AbstractEventSlotProjection implements GuestProjection {
    private LocalDate startDate;
    private LocalDate endDate;
}
