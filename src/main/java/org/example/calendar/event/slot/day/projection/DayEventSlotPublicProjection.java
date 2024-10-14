package org.example.calendar.event.slot.day.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.projection.AbstractEventSlotPublicProjection;
import org.example.calendar.event.slot.projection.GuestProjection;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DayEventSlotPublicProjection extends AbstractEventSlotPublicProjection implements GuestProjection {
    private LocalDate startDate;
    private LocalDate endDate;
}
