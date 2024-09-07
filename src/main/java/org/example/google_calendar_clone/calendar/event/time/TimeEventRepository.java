package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.entity.TimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TimeEventRepository extends JpaRepository<TimeEvent, UUID> {
}
