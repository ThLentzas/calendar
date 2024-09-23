package org.example.google_calendar_clone.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.example.google_calendar_clone.calendar.event.AbstractEvent;

@Entity
@Table(name = "time_events")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TimeEvent extends AbstractEvent {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
    @OneToMany(mappedBy = "timeEvent")
    private Set<TimeEventSlot> timeEventSlots;
}
