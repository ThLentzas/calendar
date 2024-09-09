package org.example.google_calendar_clone.calendar.event.time.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.TimeEventSlot;

import java.util.List;
import java.util.UUID;

interface TimeEventSlotRepository extends JpaRepository<TimeEventSlot, UUID> {
    // LEFT JOIN on guestEmails because we might not have invited anyone when we initially created the event
    @Query("""
                SELECT tes
                FROM TimeEventSlot tes
                JOIN FETCH tes.timeEvent te
                JOIN FETCH te.user
                LEFT JOIN FETCH tes.guestEmails
                WHERE tes.timeEvent.id = :id
                ORDER BY tes.startTime
            """)
    List<TimeEventSlot> findByEventId(@Param("id") UUID id);
}
