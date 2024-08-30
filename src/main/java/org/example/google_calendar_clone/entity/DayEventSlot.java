package org.example.google_calendar_clone.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.google_calendar_clone.calendar.event.AbstractEventSlot;

import java.time.LocalDate;

@Entity
@Table(name = "day_event_slots")
@Getter
@Setter
public class DayEventSlot extends AbstractEventSlot {
    private LocalDate startDate;
    private LocalDate endDate;
    @ManyToOne(fetch = FetchType.LAZY)
    private DayEvent dayEvent;
}
