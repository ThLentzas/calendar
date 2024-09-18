package org.example.google_calendar_clone.calendar.event.day.slot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.slot.AbstractEventSlotReminderEmailRequest;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DayEventSlotReminderRequest extends AbstractEventSlotReminderEmailRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
