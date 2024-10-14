package org.example.calendar.event.slot.time;

import org.example.calendar.entity.TimeEventSlot;
import org.example.calendar.event.slot.projection.EventSlotWithGuestsProjection;
import org.example.calendar.event.slot.projection.mapper.EventSlotWithGuestsProjectionRowMapper;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.example.calendar.event.slot.time.projection.TimeEventSlotProjection;
import org.example.calendar.event.slot.time.projection.TimeEventSlotReminderProjection;
import org.example.calendar.event.slot.time.projection.mapper.TimeEventSlotProjectionRowMapper;
import org.example.calendar.event.slot.time.projection.mapper.TimeEventSlotPublicProjectionRowMapper;
import org.example.calendar.event.slot.time.projection.mapper.TimeEventSlotReminderProjectionRowMapper;
import org.example.calendar.utils.EventUtils;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
public class TimeEventSlotRepository {
    private final JdbcClient jdbcClient;

    void create(TimeEventSlot eventSlot) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcClient.sql("""
                        INSERT INTO time_event_slots(event_id, title, location, description, start_time, start_time_zone_id, end_time, end_time_zone_id)
                        VALUES (:eventId, :title, :location, :description, :startTime, :startTimeZoneId, :endTime, :endTimeZoneId)
                        """)
                .param("eventId", eventSlot.getEventId())
                .param("title", eventSlot.getTitle())
                .param("location", eventSlot.getLocation())
                .param("description", eventSlot.getDescription())
                .param("startTime", eventSlot.getStartTime())
                .param("startTimeZoneId", eventSlot.getStartTimeZoneId().toString())
                .param("endTime", eventSlot.getEndTime())
                .param("endTimeZoneId", eventSlot.getEndTimeZoneId().toString())
                .update(keyHolder, "id");

        eventSlot.setId(keyHolder.getKeyAs(UUID.class));

        for (String guestEmail : eventSlot.getGuestEmails()) {
            this.jdbcClient.sql("""
                            INSERT INTO time_event_slot_guest_emails(event_slot_id, email)
                            VALUES (:eventSlotId, :email)
                            """)
                    .param("eventSlotId", eventSlot.getId())
                    .param("email", guestEmail)
                    .update();
        }
    }

    void update(TimeEventSlot original, TimeEventSlot modified) {
        StringBuilder sql = new StringBuilder("UPDATE time_event_slots SET ");
        Map<String, Object> params = new HashMap<>();
        EventUtils.updateDateTimeProperties(sql, original.getStartTime(), original.getStartTimeZoneId(), modified.getStartTime(), modified.getStartTimeZoneId(), original.getEndTime(), original.getEndTimeZoneId(), modified.getEndTime(), modified.getEndTimeZoneId(), params);
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

    void updateEventSlotForEvent(TimeEventSlot original, TimeEventSlot modified) {
        StringBuilder sql = new StringBuilder("UPDATE time_event_slots SET ");
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
                            INSERT INTO time_event_slot_guest_emails(event_slot_id, email)
                            VALUES (:eventSlotId, :email)
                            """)
                    .param("eventSlotId", slotId)
                    .param("email", guestEmail)
                    .update();
        }
    }

    // We return a list from our query, because if the event slot has more than 1 guest email, we will have 1 row with
    // the same event slot but different email each time. We handle the case of conversion to a single event slot with
    // a list of emails on the aggregateGuestEmails()
    Optional<TimeEventSlotProjection> findBySlotAndUserId(UUID slotId, Long userId) {
        List<TimeEventSlotProjection> results = this.jdbcClient.sql("""
                        SELECT
                            tes.id AS event_slot_id,
                            tes.start_time,
                            tes.start_time_zone_id,
                            tes.end_time,
                            tes.end_time_zone_id,
                            tes.title,
                            tes.description,
                            tes.location,
                            ge.email
                        FROM time_event_slots tes
                        JOIN time_events te ON tes.event_id = te.id
                        LEFT JOIN time_event_slot_guest_emails ge ON tes.id = ge.event_slot_id
                        WHERE tes.id = :slotId AND te.organizer_id = :userId
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .query(new TimeEventSlotProjectionRowMapper())
                .list();

        return EventUtils.aggregateGuestEmails(results);
    }

    // We return a list from our query, because if the event slot has more than 1 guest email, we will have 1 row with
    // the same event slot but different email each time. We handle the case of conversion to a single event slot with
    // a list of emails on the aggregateGuestEmails()
    Optional<EventSlotWithGuestsProjection> findBySlotAndUserIdFetchingGuests(UUID slotId, Long userId) {
        List<EventSlotWithGuestsProjection> results = this.jdbcClient.sql("""
                        SELECT
                            tes.id,
                            ge.email
                        FROM time_event_slots tes
                        JOIN time_events te ON tes.event_id = te.id
                        LEFT JOIN time_event_slot_guest_emails ge ON tes.id = ge.event_slot_id
                        WHERE tes.id = :slotId AND te.organizer_id = :userId
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .query(new EventSlotWithGuestsProjectionRowMapper())
                .list();

        return EventUtils.aggregateGuestEmails(results);
    }

    public List<TimeEventSlotPublicProjection> findByEventAndUserId(UUID eventId, Long userId) {
        List<TimeEventSlotPublicProjection> results = this.jdbcClient.sql("""
                        SELECT
                            tes.id,
                            tes.event_id,
                            tes.start_time,
                            tes.start_time_zone_id,
                            tes.end_time,
                            tes.end_time_zone_id,
                            tes.title,
                            tes.description,
                            tes.location,
                            ge.email,
                            u.username
                        FROM time_event_slots tes
                        JOIN time_events te ON tes.event_id = te.id
                        JOIN users u ON te.organizer_id = u.id
                        LEFT JOIN time_event_slot_guest_emails ge ON tes.id = ge.event_slot_id
                        WHERE tes.event_id = :eventId AND te.organizer_id = :userId
                        ORDER BY tes.start_time
                        """)
                .param("eventId", eventId)
                .param("userId", userId)
                .query(new TimeEventSlotPublicProjectionRowMapper())
                .list();

        return EventUtils.aggregateListGuestEmails(results);
    }

    // We return a list from our query, because if the event slot has more than 1 guest email, we will have 1 row with
    // the same event slot but different email each time. We handle the case of conversion to a single event slot with
    // a list of emails on the aggregateGuestEmails()
    Optional<TimeEventSlotPublicProjection> findByOrganizerOrGuestEmailAndSlotId(UUID slotId, Long userId, String email) {
        List<TimeEventSlotPublicProjection> results = this.jdbcClient.sql("""
                        SELECT
                            tes.id,
                            tes.event_id,
                            tes.start_time,
                            tes.start_time_zone_id,
                            tes.end_time,
                            tes.end_time_zone_id,
                            tes.title,
                            tes.description,
                            tes.location,
                            ge.email,
                            u.username
                        FROM time_event_slots tes
                        JOIN time_events te ON tes.event_id = te.id
                        JOIN users u ON te.organizer_id = u.id
                        LEFT JOIN time_event_slot_guest_emails ge ON tes.id = ge.event_slot_id
                        WHERE (te.organizer_id = :userId OR ge.email = :email) AND tes.id = :slotId
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .param("email", email)
                .query(new TimeEventSlotPublicProjectionRowMapper())
                .list();

        return EventUtils.aggregateGuestEmails(results);
    }

    List<TimeEventSlotPublicProjection> findByUserInDateRange(Long userId, String email, LocalDateTime startTime, LocalDateTime endTime) {
        List<TimeEventSlotPublicProjection> results = this.jdbcClient.sql("""
                        SELECT
                            tes.id,
                            tes.event_id,
                            tes.start_time,
                            tes.start_time_zone_id,
                            tes.end_time,
                            tes.end_time_zone_id,
                            tes.title,
                            tes.description,
                            tes.location,
                            ge.email,
                            u.username
                        FROM time_event_slots tes
                        JOIN time_events te ON tes.event_id = te.id
                        JOIN users u ON te.organizer_id = u.id
                        LEFT JOIN time_event_slot_guest_emails ge ON tes.id = ge.event_slot_id
                        WHERE (te.organizer_id = :userId OR ge.email = :email) AND tes.start_time BETWEEN :startTime AND :endTime
                        ORDER BY tes.start_time
                        """)
                .param("userId", userId)
                .param("email", email)
                .param("startTime", Timestamp.valueOf(startTime))
                .param("endTime", Timestamp.valueOf(endTime))
                .query(new TimeEventSlotPublicProjectionRowMapper())
                .list();

        return EventUtils.aggregateListGuestEmails(results);
    }

    public List<TimeEventSlotReminderProjection> findByStartTime(LocalDateTime startTime) {
        List<TimeEventSlotReminderProjection> results = this.jdbcClient.sql("""
                        SELECT
                            tes.id,
                            tes.start_time,
                            tes.end_time,
                            tes.title,
                            ge.email AS guest_email,
                            u.username,
                            u.email AS organizer_email
                        FROM time_event_slots tes
                        JOIN time_events te ON tes.event_id = te.id
                        JOIN users u ON te.organizer_id = u.id
                        LEFT JOIN time_event_slot_guest_emails ge ON tes.id = ge.event_slot_id
                        WHERE tes.start_time = :startTime
                        ORDER BY tes.start_time
                        """)
                .param("startTime", Timestamp.valueOf(startTime))
                .query(new TimeEventSlotReminderProjectionRowMapper())
                .list();

        return EventUtils.aggregateListGuestEmails(results);
    }

    /*
        We can not use JOIN with DELETE we need to use a sub-query
     */
    int deleteBySlotAndUserId(UUID slotId, Long userId) {
        return this.jdbcClient.sql("""
                            DELETE FROM time_event_slots tes
                            WHERE tes.id = :slotId AND tes.event_id IN (
                                SELECT te.id
                                FROM time_events te
                                WHERE te.id = tes.event_id AND te.organizer_id = :userId);
                        """)
                .param("slotId", slotId)
                .param("userId", userId)
                .update();
    }

    void deleteEventSlotsByEventId(UUID eventId) {
        this.jdbcClient.sql("""
                            DELETE FROM time_event_slots
                            WHERE event_id = :eventId
                        """)
                .param("eventId", eventId)
                .update();
    }

    private void updateGuests(UUID slotId, Set<String> guestEmails) {
        this.jdbcClient.sql("""
                        DELETE
                        FROM time_event_slot_guest_emails
                        WHERE event_slot_id = :slotId
                        """)
                .param("slotId", slotId)
                .update();

        for (String guestEmail : guestEmails) {
            this.jdbcClient.sql("""
                            INSERT INTO time_event_slot_guest_emails(event_slot_id, email)
                            VALUES (:slotId, :email)
                            """)
                    .param("slotId", slotId)
                    .param("email", guestEmail)
                    .update();
        }
    }
}
