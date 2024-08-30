package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.entity.DayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DayEventRepository extends JpaRepository<DayEvent, UUID> {
}
