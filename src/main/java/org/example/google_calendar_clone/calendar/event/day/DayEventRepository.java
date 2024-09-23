package org.example.google_calendar_clone.calendar.event.day;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.DayEvent;

import java.util.Optional;
import java.util.UUID;

public interface DayEventRepository extends JpaRepository<DayEvent, UUID> {

    @Query("""
                SELECT de
                FROM DayEvent de
                JOIN FETCH de.dayEventSlots des
                LEFT JOIN FETCH des.guestEmails
                WHERE de.id = :eventId AND de.user.id = :userId
            """)
    Optional<DayEvent> findByEventAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

    // returns affected rows
    @Modifying
    @Query("""
                DELETE FROM DayEvent de
                WHERE de.id = :eventId AND de.user.id = :userId
            """)
    int deleteByEventAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);
}
