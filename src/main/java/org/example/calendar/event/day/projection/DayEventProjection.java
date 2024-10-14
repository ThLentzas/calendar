package org.example.calendar.event.day.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.AbstractEventProjection;
import org.example.calendar.event.slot.day.projection.DayEventSlotProjection;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DayEventProjection extends AbstractEventProjection {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DayEventSlotProjection> eventSlots;
}
