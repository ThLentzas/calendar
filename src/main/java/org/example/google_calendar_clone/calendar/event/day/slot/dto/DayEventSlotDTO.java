package org.example.google_calendar_clone.calendar.event.day.slot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.example.google_calendar_clone.calendar.event.AbstractEventSlotDTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DayEventSlotDTO extends AbstractEventSlotDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
