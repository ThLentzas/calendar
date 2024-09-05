package org.example.google_calendar_clone.calendar.event.day.slot.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.example.google_calendar_clone.calendar.event.AbstractEventSlotDTO;

@Getter
@Setter
public class DayEventSlotDTO extends AbstractEventSlotDTO {
    private LocalDate startDate;
    private LocalDate endDate;

    public DayEventSlotDTO(UUID id, String name, LocalDate startDate, LocalDate endDate, String location , String description, Set<String> guestEmails) {
        super(id, name, location, description, guestEmails);
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
