package org.example.google_calendar_clone.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.google_calendar_clone.calendar.AbstractCalendarEvent;

import java.time.LocalDate;

import jakarta.persistence.Entity;

@Entity
@Getter
@Setter
public class DayEvent extends AbstractCalendarEvent {
    private LocalDate startDate;
    private LocalDate endDate;
}
