package org.example.google_calendar_clone.entity;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import lombok.experimental.SuperBuilder;
import org.example.google_calendar_clone.calendar.event.AbstractEvent;

import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "time_events")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TimeEvent extends AbstractEvent {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @OneToMany(mappedBy = "timeEvent")
    private Set<TimeEventSlot> timeEventSlots;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
}
