package org.example.google_calendar_clone.calendar.event.day.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.example.google_calendar_clone.entity.DayEventSlot;

interface DayEventSlotRepository extends JpaRepository<DayEventSlot, UUID> {
    // LEFT JOIN on guestEmails because we might not have invited anyone when we initially created the event
    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                JOIN FETCH de.user
                LEFT JOIN FETCH des.guestEmails
                WHERE des.dayEvent.id = :id
                ORDER BY des.startDate
            """)
    List<DayEventSlot> findByEventId(@Param("id") UUID id);

    /*
    IN vs MEMBER OF
    https://stackoverflow.com/questions/5915822/whats-the-difference-between-the-in-and-member-of-jpql-operators

    For IN, we need a case where tes.someValue IN some collection parameter. Also, it does not work directly with
    @ElementCollection
 */
    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                JOIN FETCH de.user
                LEFT JOIN FETCH des.guestEmails
                WHERE (de.user = :user OR :email MEMBER OF des.guestEmails) AND des.startDate BETWEEN :startDate AND :endDate
                ORDER BY des.startDate
            """)
    List<DayEventSlot> findByUserInDateRange(@Param("user") User user,
                                             @Param("email") String email,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent
                LEFT JOIN FETCH des.guestEmails
                WHERE des.id = :id
            """)
    Optional<DayEventSlot> findBySlotId(@Param("id") UUID id);

}
