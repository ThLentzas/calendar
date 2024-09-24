package org.example.calendar.event.slot.day;

import org.example.calendar.entity.DayEventSlot;
import org.example.calendar.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DayEventSlotRepository extends JpaRepository<DayEventSlot, UUID> {
    // LEFT JOIN on guestEmails because we might not have invited anyone when we initially created the event
    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                JOIN FETCH de.user
                LEFT JOIN FETCH des.guestEmails
                WHERE des.dayEvent.id = :eventId AND de.user.id = :userId
                ORDER BY des.startDate
            """)
    List<DayEventSlot> findByEventAndUserId(@Param("eventId") UUID eventId, @Param("userId") Long userId);

    /*
        IN vs MEMBER OF
        https://stackoverflow.com/questions/5915822/whats-the-difference-between-the-in-and-member-of-jpql-operators

        For IN, we need a case where des.someValue IN some collection parameter. Also, it does not work directly with
        @ElementCollection
     */
    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                JOIN FETCH de.user
                LEFT JOIN FETCH des.guestEmails
                WHERE (de.user.id = :userId OR :email MEMBER OF des.guestEmails) AND des.startDate BETWEEN :startDate AND :endDate
                ORDER BY des.startDate
            """)
    List<DayEventSlot> findByUserInDateRange(@Param("userId") Long userId,
                                             @Param("email") String email,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                JOIN FETCH de.user
                LEFT JOIN FETCH des.guestEmails
                WHERE (de.user.id = :userId OR :email MEMBER OF des.guestEmails) AND des.id = :slotId
            """)
    Optional<DayEventSlot> findByOrganizerOrGuestEmailAndSlotId(@Param("userId") Long userId,
                                                                @Param("email") String email,
                                                                @Param("slotId") UUID slotId);

    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                JOIN FETCH de.user
                LEFT JOIN FETCH des.guestEmails
                WHERE des.startDate = :startDate
            """)
    List<DayEventSlot> findByStartDate(@Param("startDate") LocalDate startDate);

    @Query("""
                SELECT des
                FROM DayEventSlot des
                JOIN FETCH des.dayEvent de
                LEFT JOIN FETCH des.guestEmails
                WHERE des.id = :slotId AND de.user.id = :userId
            """)
    Optional<DayEventSlot> findBySlotAndUserId(@Param("slotId") UUID slotId, @Param("userId") Long userId);

    /*
        Two delete queries will be logged, first to delete all the guest emails and then the slot itself
        WHERE des.id = :slotId AND des.dayEvent.user.id = :userId would not work

        Hibernate:
            delete
            from
                day_event_slots des1_0
            where
                des1_0.id=?
                and des1_0.id in (select
                    des2_0.id
                from
                    day_event_slots des2_0
                join
                    day_events de1_0
                        on de1_0.id=des2_0.day_event_id
                where
                    de1_0.user_id=?)

        The subquery checks if the DayEventSlot with the given slotId is owned by a specific user (userId). The subquery
        returns all DayEventSlot Ids where the dayEvent is associated with the given user.

        Returns affected rows
     */
    @Modifying
    @Query("""
                DELETE FROM DayEventSlot des
                    WHERE des.id = :slotId
                    AND des.id IN (
                        SELECT d.id FROM DayEventSlot d
                        WHERE d.dayEvent.user.id = :userId)
            """)
    int deleteBySlotAndUserId(@Param("slotId") UUID slotId, @Param("userId") Long userId);

    default DayEventSlot findByIdOrThrow(UUID slotId, Long userId) {
        return findBySlotAndUserId(slotId, userId).orElseThrow(() -> new ResourceNotFoundException("Day event slot not found with id: " + slotId));
    }
}
