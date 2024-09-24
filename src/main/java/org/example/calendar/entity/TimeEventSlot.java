package org.example.calendar.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.example.calendar.event.slot.AbstractEventSlot;

@Entity
@Table(name = "time_event_slots")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class TimeEventSlot extends AbstractEventSlot {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(updatable = false)
    private TimeEvent timeEvent;
    /*
        Element collections is used over OneToMany, because we have a Set<String> not an Entity.

        The guestEmails can't be shared from the super class. When we create the guest_emails table and, we want to
        reference the event_id, we don't know which table to reference. If the event is a DayEvent we would have to
        reference the day_events table but if it is TimeEvent we need the time_events table.
    */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "time_event_slot_guest_emails", joinColumns = @JoinColumn(name = "time_event_slot_id"))
    @Column(name = "email")
    private Set<String> guestEmails;
}
