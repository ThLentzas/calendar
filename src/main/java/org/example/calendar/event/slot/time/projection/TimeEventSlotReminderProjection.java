package org.example.calendar.event.slot.time.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.projection.AbstractEventSlotReminderProjection;
import org.example.calendar.event.slot.projection.GuestProjection;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TimeEventSlotReminderProjection extends AbstractEventSlotReminderProjection implements GuestProjection {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
