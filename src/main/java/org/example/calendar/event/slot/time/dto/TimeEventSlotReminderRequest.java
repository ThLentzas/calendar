package org.example.calendar.event.slot.time.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.AbstractEventSlotReminderRequest;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TimeEventSlotReminderRequest extends AbstractEventSlotReminderRequest {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
