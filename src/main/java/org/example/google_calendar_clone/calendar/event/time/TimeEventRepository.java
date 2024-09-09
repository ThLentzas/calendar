package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.entity.TimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TimeEventRepository extends JpaRepository<TimeEvent, UUID> {
    @Query("""
                SELECT te
                FROM TimeEvent te
                JOIN FETCH te.user
                WHERE te.id = :id
            """)
    Optional<TimeEvent> findByIdFetchingUser(@Param("id") UUID id);
}

