package org.example.google_calendar_clone.calendar.event.time.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    /*
        IN vs MEMBER OF
        https://stackoverflow.com/questions/5915822/whats-the-difference-between-the-in-and-member-of-jpql-operators

        For IN, we need a case where tes.someValue IN some collection parameter. Also, it does not work directly with
        @ElementCollection
     */
    @Query("""
                SELECT tes
                FROM TimeEventSlot tes
                JOIN FETCH tes.timeEvent te
                JOIN FETCH te.user
                LEFT JOIN FETCH tes.guestEmails
                WHERE (te.user = :user OR :email MEMBER OF tes.guestEmails) AND tes.startTime BETWEEN :startTime AND :endTime
                ORDER BY tes.startTime
            """)
    List<TimeEventSlot> findByUserInDateRange(@Param("user") User user,
                                              @Param("email") String email,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    @Query("""
                SELECT tes
                FROM TimeEventSlot tes
                LEFT JOIN FETCH tes.guestEmails
                WHERE tes.id = :id
            """)
    Optional<TimeEventSlot> findBySlotId(@Param("id") UUID id);
}
