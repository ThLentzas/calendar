package org.example.calendar.event.slot.day.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.slot.AbstractEventSlotReminderRequest;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DayEventSlotReminderRequest extends AbstractEventSlotReminderRequest {
    private LocalDate startDate;
}
