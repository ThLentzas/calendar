package org.example.google_calendar_clone.calendar.event.day;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;

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

    @Query("""
                SELECT COUNT(de) > 0
                FROM DayEvent de
                WHERE de.id = :eventId AND de.user.id = :userId
            """)
    boolean existsByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

    default DayEvent findByIdOrThrow(UUID id) {
        return findByIdFetchingUser(id).orElseThrow(() -> new ResourceNotFoundException("Day event not found with id: " + id));
    }
}
