package org.example.google_calendar_clone.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.example.google_calendar_clone.calendar.event.AbstractEvent;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TimeEvent extends AbstractEvent {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
