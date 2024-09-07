package org.example.google_calendar_clone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.*;

import java.time.LocalDate;
import java.util.Set;

import lombok.experimental.SuperBuilder;
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
