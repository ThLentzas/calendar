package org.example.google_calendar_clone.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Set;

import org.example.google_calendar_clone.calendar.event.AbstractEvent;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
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
