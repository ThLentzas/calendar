package org.example.calendar.event.slot.time;

import org.example.calendar.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.calendar.entity.TimeEventSlot;
import org.example.calendar.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeEventSlotRepository extends JpaRepository<TimeEventSlot, UUID> {
    // LEFT JOIN on guestEmails because we might not have invited anyone when we initially created the event
    @Query("""
                SELECT tes
                FROM TimeEventSlot tes
                JOIN FETCH tes.timeEvent te
                JOIN FETCH te.user
                LEFT JOIN FETCH tes.guestEmails
                WHERE tes.timeEvent.id = :eventId AND te.user.id = :userId
                ORDER BY tes.startTime
            """)
    List<TimeEventSlot> findByEventAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

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
                JOIN FETCH tes.timeEvent te
                JOIN FETCH te.user
                LEFT JOIN FETCH tes.guestEmails
                WHERE (te.user.id = :userId OR :email MEMBER OF tes.guestEmails) AND tes.id = :slotId
            """)
    Optional<TimeEventSlot> findByOrganizerOrGuestEmailAndSlotId(@Param("userId") Long userId,
                                                                 @Param("email") String email,
                                                                 @Param("slotId") UUID slotId);

    @Query("""
                SELECT tes
                FROM TimeEventSlot tes
                JOIN FETCH tes.timeEvent te
                JOIN FETCH te.user
                LEFT JOIN FETCH tes.guestEmails
                WHERE tes.startTime = :startTime
            """)
    List<TimeEventSlot> findByStartTime(@Param("startTime") LocalDateTime startTime);

    @Query("""
                SELECT tes
                FROM TimeEventSlot tes
                JOIN FETCH tes.timeEvent te
                LEFT JOIN FETCH tes.guestEmails
                WHERE tes.id = :slotId AND te.user.id = :userId
            """)
    Optional<TimeEventSlot> findBySlotAndUserId(@Param("slotId") UUID slotId, @Param("userId") Long userId);

    /*
        Two delete queries will be logged, first to delete all the guest emails and then the slot itself
        WHERE des.id = :slotId AND tes.timeEvent.user.id = :userId would not work
        Hibernate:
            delete
            from
                time_event_slot_guest_emails to_delete_
            where
                to_delete_.time_event_slot_id in (select
                    tes1_0.id
                from
                    time_event_slots tes1_0
                where
                    tes1_0.id=?
                    and tes1_0.id in (select
                        tes2_0.id
                    from
                        time_event_slots tes2_0
                    join
                        time_events te1_0
                            on te1_0.id=tes2_0.time_event_id
                    where
                        te1_0.user_id=?))
        Hibernate:
            delete
            from
                time_event_slots tes1_0
            where
                tes1_0.id=?
                and tes1_0.id in (select
                    tes2_0.id
                from
                    time_event_slots tes2_0
                join
                    time_events te1_0
                        on te1_0.id=tes2_0.time_event_id
                where
                    te1_0.user_id=?)

        The subquery checks if the TimeEventSlot with the given slotId is owned by a specific user (userId). The subquery
        returns all TimeEventSlot Ids where the timeEvent is associated with the given user.

        Returns affected rows
     */
    @Modifying
    @Query("""
                DELETE FROM TimeEventSlot tes
                    WHERE tes.id = :slotId
                    AND tes.id IN (
                        SELECT t.id FROM TimeEventSlot t
                        WHERE t.timeEvent.user.id = :userId)
            """)
    int deleteBySlotAndUserId(@Param("slotId") UUID slotId, @Param("userId") Long userId);

    default TimeEventSlot findByIdOrThrow(UUID slotId, Long userId) {
        return findBySlotAndUserId(slotId, userId).orElseThrow(() -> new ResourceNotFoundException("Time event slot not found with id: " + slotId));
    }
}
