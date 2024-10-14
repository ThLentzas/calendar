package org.example.calendar.event.slot.day.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.projection.AbstractEventSlotReminderProjection;
import org.example.calendar.event.slot.projection.GuestProjection;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DayEventSlotReminderProjection extends AbstractEventSlotReminderProjection implements GuestProjection {
    private LocalDate startDate;
}
