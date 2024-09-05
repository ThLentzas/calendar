package org.example.google_calendar_clone.calendar.event.day.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import org.example.google_calendar_clone.entity.DayEventSlot;

interface DayEventSlotRepository extends JpaRepository<DayEventSlot, UUID> {
    // LEFT JOIN on guestEmails because we might not have invited anyone when we initially created the event
    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent
                LEFT JOIN FETCH des.guestEmails
                WHERE des.dayEvent.id = :id
                ORDER BY des.startDate
            """)
    List<DayEventSlot> findByEventId(@Param("id") UUID id);

}
