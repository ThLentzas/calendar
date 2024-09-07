package org.example.google_calendar_clone.calendar.event.time.slot;

import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimeEventSlotRepository extends JpaRepository<TimeEventSlot, UUID> {
}
