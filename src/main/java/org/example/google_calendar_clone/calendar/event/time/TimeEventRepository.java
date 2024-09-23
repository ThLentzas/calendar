package org.example.google_calendar_clone.calendar.event.time;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.TimeEvent;

import java.util.Optional;
import java.util.UUID;

public interface TimeEventRepository extends JpaRepository<TimeEvent, UUID> {

    @Query("""
                SELECT te
                FROM TimeEvent te
                JOIN FETCH te.timeEventSlots tes
                LEFT JOIN FETCH tes.guestEmails
                WHERE te.id = :eventId AND te.user.id = :userId
            """)
    Optional<TimeEvent> findByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

    // returns affected rows
    @Modifying
    @Query("""
                DELETE FROM TimeEvent te
                WHERE te.id = :eventId AND te.user.id = :userId
            """)
    int deleteByEventAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

}

