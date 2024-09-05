package org.example.google_calendar_clone.calendar.event.day;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.DayEvent;

import java.util.Optional;
import java.util.UUID;

public interface DayEventRepository extends JpaRepository<DayEvent, UUID> {
    @Query("""
                SELECT de
                FROM DayEvent de
                JOIN FETCH de.user
                WHERE de.id = :id
            """)
    Optional<DayEvent> findByIdFetchingUser(@Param("id") UUID id);
}
