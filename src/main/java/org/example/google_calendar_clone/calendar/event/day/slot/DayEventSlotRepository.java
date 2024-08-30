package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.entity.DayEventSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DayEventSlotRepository extends JpaRepository<DayEventSlot, UUID> {
}
