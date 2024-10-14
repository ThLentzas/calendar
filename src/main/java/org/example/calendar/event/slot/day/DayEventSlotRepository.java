package org.example.calendar.event.slot.day;

import org.example.calendar.entity.DayEventSlot;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.slot.projection.EventSlotWithGuestsProjection;
import org.example.calendar.event.slot.day.projection.mapper.DayEventSlotPublicProjectionRowMapper;
import org.example.calendar.event.slot.day.projection.DayEventSlotProjection;
import org.example.calendar.event.slot.day.projection.mapper.DayEventSlotProjectionRowMapper;
import org.example.calendar.event.slot.day.projection.DayEventSlotReminderProjection;
import org.example.calendar.event.slot.day.projection.mapper.DayEventSlotReminderProjectionRowMapper;
import org.example.calendar.event.slot.projection.mapper.EventSlotWithGuestsProjectionRowMapper;
import org.example.calendar.utils.EventUtils;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DayEventSlotRepository {
    private final JdbcClient jdbcClient;

    void create(DayEventSlot eventSlot) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcClient.sql("""
                        INSERT INTO day_event_slots(event_id, title, location, description, start_date, end_date)
                        VALUES (:eventId, :title, :location, :description, :startDate, :endDate)
                        """)
                .param("eventId", eventSlot.getEventId())
                .param("title", eventSlot.getTitle())
                .param("location", eventSlot.getLocation())
                .param("description", eventSlot.getDescription())
                .param("startDate", Date.valueOf(eventSlot.getStartDate()))
                .param("endDate", Date.valueOf(eventSlot.getEndDate()))
                .update(keyHolder, "id");

        eventSlot.setId(keyHolder.getKeyAs(UUID.class));

        for (String guestEmail : eventSlot.getGuestEmails()) {
            this.jdbcClient.sql("""
                            INSERT INTO day_event_slot_guest_emails(event_slot_id, email)
                            VALUES (:eventSlotId, :email)
                            """)
                    .param("eventSlotId", eventSlot.getId())
                    .param("email", guestEmail)
                    .update();
        }
    }

    void update(DayEventSlot original, DayEventSlot modified) {
        StringBuilder sql = new StringBuilder("UPDATE day_event_slots SET ");
        Map<String, Object> params = new HashMap<>();
        EventUtils.updateDateProperties(sql, original.getStartDate(), modified.getStartDate(), original.getEndDate(), modified.getEndDate(), params);
        EventUtils.updateCommonEvenSlotProperties(sql, original, modified, params);
        if (!Objects.equals(original.getGuestEmails(), modified.getGuestEmails())) {
            updateGuests(original.getId(), modified.getGuestEmails());
        }

        // If any properties were updated, execute the update
        if (!params.isEmpty()) {
            // Remove the last ", "
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = :slotId");
            params.put("slotId", original.getId());
            this.jdbcClient.sql(sql.toString())
                    .params(params)
                    .update();
        }
    }

    // We don't update date related properties. This method updates properties like title, description, location of all
    // event slots for a given event. To update the start date and end date of all event slots user provided different
    // frequencies which means we need to delete the previous slots and compute the new ones
    void updateEventSlotForEvent(DayEventSlot original, DayEventSlot modified) {
        StringBuilder sql = new StringBuilder("UPDATE day_event_slots SET ");
        Map<String, Object> params = new HashMap<>();
        EventUtils.updateCommonEvenSlotProperties(sql, original, modified, params);
        if (!Objects.equals(original.getGuestEmails(), modified.getGuestEmails())) {
            updateGuests(original.getId(), modified.getGuestEmails());
        }

        // If any properties were updated, execute the update
        if (!params.isEmpty()) {
            // Remove the last ", "
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = :slotId");
            params.put("slotId", original.getId());
            this.jdbcClient.sql(sql.toString())
                    .params(params)
                    .update();
        }
    }

    /*
        We can't pass directly a day event slot instance and iterate the guest emails, because the set will contain
        all the guests for the current slot, the previous ones and the ones we just added. Instead, we pass the list of
        the new guest emails to be added. In theory, we could and in our INSERT call DO NOTHING ON CONFLICT.
     */
    void inviteGuests(UUID slotId, Set<String> guestEmails) {
        for (String guestEmail : guestEmails) {
            this.jdbcClient.sql("""
                            INSERT INTO day_event_slot_guest_emails(event_slot_id, email)
                            VALUES (:eventSlotId, :email)
                            """)
                    .param("eventSlotId", slotId)
                    .param("email", guestEmail)
                    .update();
        }
    }

    Optional<DayEventSlotProjection> findBySlotAndUserId(UUID slotId, Long userId) {
        List<DayEventSlotProjection> results = this.jdbcClient.sql("""
                        SELECT
                            des.id AS event_slot_id,
                            des.start_date,
                            des.end_date,
                            des.title,
                            des.description,
                            des.location,
                            ge.email
                        FROM day_event_slots des
                        JOIN day_events de ON des.event_id = de.id
                        LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                        WHERE des.id = :slotId AND de.organizer_id = :userId
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .query(new DayEventSlotProjectionRowMapper())
                .list();

        return EventUtils.aggregateGuestEmails(results);
    }

    Optional<EventSlotWithGuestsProjection> findBySlotAndUserIdFetchingGuests(UUID slotId, Long userId) {
        List<EventSlotWithGuestsProjection> results = this.jdbcClient.sql("""
                        SELECT
                            des.id,
                            ge.email
                        FROM day_event_slots des
                        JOIN day_events de ON des.event_id = de.id
                        LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                        WHERE des.id = :slotId AND de.organizer_id = :userId
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .query(new EventSlotWithGuestsProjectionRowMapper())
                .list();

        return EventUtils.aggregateGuestEmails(results);
    }

    public List<DayEventSlotPublicProjection> findByEventAndUserId(UUID eventId, Long userId) {
        List<DayEventSlotPublicProjection> results = this.jdbcClient.sql("""
                        SELECT
                            des.id,
                            des.event_id,
                            des.start_date,
                            des.end_date,
                            des.title,
                            des.description,
                            des.location,
                            ge.email,
                            u.username
                        FROM day_event_slots des
                        JOIN day_events de ON des.event_id = de.id
                        JOIN users u ON de.organizer_id = u.id
                        LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                        WHERE des.event_id = :eventId AND de.organizer_id = :userId
                        ORDER BY des.start_date
                        """)
                .param("eventId", eventId)
                .param("userId", userId)
                .query(new DayEventSlotPublicProjectionRowMapper())
                .list();

        return EventUtils.aggregateListGuestEmails(results);
    }

    Optional<DayEventSlotPublicProjection> findByOrganizerOrGuestEmailAndSlotId(UUID slotId, Long userId, String email) {
        List<DayEventSlotPublicProjection> results = this.jdbcClient.sql("""
                        SELECT
                            des.id,
                            des.event_id,
                            des.start_date,
                            des.end_date,
                            des.title,
                            des.description,
                            des.location,
                            ge.email,
                            u.username
                        FROM day_event_slots des
                        JOIN day_events de ON des.event_id = de.id
                        JOIN users u ON de.organizer_id = u.id
                        LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                        WHERE (de.organizer_id = :userId OR ge.email = :email) AND des.id = :slotId
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .param("email", email)
                .query(new DayEventSlotPublicProjectionRowMapper())
                .list();

        return EventUtils.aggregateGuestEmails(results);
    }

    List<DayEventSlotPublicProjection> findByUserInDateRange(Long userId, String email, LocalDate startDate, LocalDate endDate) {
        List<DayEventSlotPublicProjection> results = this.jdbcClient.sql("""
                        SELECT
                            des.id,
                            des.event_id,
                            des.start_date,
                            des.end_date,
                            des.title,
                            des.description,
                            des.location,
                            ge.email,
                            u.username
                        FROM day_event_slots des
                        JOIN day_events de ON des.event_id = de.id
                        JOIN users u ON de.organizer_id = u.id
                        LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                        WHERE (de.organizer_id = :userId OR ge.email = :email) AND des.start_date BETWEEN :startDate AND :endDate
                        ORDER BY des.start_date
                        """)
                .param("userId", userId)
                .param("email", email)
                .param("startDate", startDate)
                .param("endDate", endDate)
                .query(new DayEventSlotPublicProjectionRowMapper())
                .list();

        return EventUtils.aggregateListGuestEmails(results);
    }

    public List<DayEventSlotReminderProjection> findByStartDate(LocalDate startDate) {
        // Conflict if we fetch ge.email and u.email
        List<DayEventSlotReminderProjection> results = this.jdbcClient.sql("""
                        SELECT
                            des.id,
                            des.start_date,
                            des.title,
                            ge.email AS guest_email,
                            u.username,
                            u.email AS organizer_email
                        FROM day_event_slots des
                        JOIN day_events de ON des.event_id = de.id
                        JOIN users u ON de.organizer_id = u.id
                        LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                        WHERE des.start_date = :startDate
                        ORDER BY des.start_date
                        """)
                .param("startDate", Date.valueOf(startDate))
                .query(new DayEventSlotReminderProjectionRowMapper())
                .list();

        return EventUtils.aggregateListGuestEmails(results);
    }

    /*
        We can not use JOIN with DELETE we need to use a sub-query
     */
    int deleteBySlotAndUserId(UUID slotId, Long userId) {
        return this.jdbcClient.sql("""
                            DELETE FROM day_event_slots des
                            WHERE des.id = :slotId AND des.event_id IN (
                                SELECT de.id
                                FROM day_events de
                                WHERE de.id = des.event_id AND de.organizer_id = :userId);
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .update();
    }

    void deleteEventSlotsByEventId(UUID eventId) {
        this.jdbcClient.sql("""
                            DELETE FROM day_event_slots
                            WHERE event_id = :eventId
                        """)
                .param("eventId", eventId)
                .update();
    }

    private void updateGuests(UUID slotId, Set<String> guestEmails) {
        this.jdbcClient.sql("""
                        DELETE
                        FROM day_event_slot_guest_emails
                        WHERE event_slot_id = :slotId
                        """)
                .param("slotId", slotId)
                .update();

        // Empty, can't be null
        for (String guestEmail : guestEmails) {
            this.jdbcClient.sql("""
                            INSERT INTO day_event_slot_guest_emails(event_slot_id, email)
                            VALUES (:slotId, :email)
                            """)
                    .param("slotId", slotId)
                    .param("email", guestEmail)
                    .update();
        }
    }
}
