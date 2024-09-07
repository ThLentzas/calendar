package org.example.google_calendar_clone.calendar.event.time.slot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.google_calendar_clone.calendar.event.EventSlotDTO;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TimeEventSlotDTO extends EventSlotDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
