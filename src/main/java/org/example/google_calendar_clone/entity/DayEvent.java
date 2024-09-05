package org.example.google_calendar_clone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

import org.example.google_calendar_clone.calendar.event.AbstractEvent;

@Entity
@Table(name = "day_events")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DayEvent extends AbstractEvent {
    private LocalDate startDate;
    private LocalDate endDate;
    @OneToMany(mappedBy = "dayEvent")
    private Set<DayEventSlot> dayEventSlots;
}
