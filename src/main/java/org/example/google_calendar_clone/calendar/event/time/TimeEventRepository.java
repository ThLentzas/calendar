package org.example.google_calendar_clone.calendar.event.time;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;

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

    @Query("""
                SELECT COUNT(te) > 0
                FROM TimeEvent te
                WHERE te.id = :eventId AND te.user.id = :userId
            """)
    boolean existsByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

    default TimeEvent findByIdOrThrow(UUID id) {
        return findByIdFetchingUser(id).orElseThrow(() -> new ResourceNotFoundException("Time event not found with id: " + id));
    }
}

