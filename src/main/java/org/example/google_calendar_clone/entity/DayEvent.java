package org.example.google_calendar_clone.entity;

import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.example.google_calendar_clone.calendar.event.AbstractEvent;

import java.time.LocalDate;

import jakarta.persistence.Entity;

@Entity
@Table(name = "day_events")
@Getter
@Setter
public class DayEvent extends AbstractEvent {
    private LocalDate startDate;
    private LocalDate endDate;
}
