package org.example.calendar.event.day;

import org.example.calendar.entity.DayEvent;
import org.example.calendar.event.day.projection.DayEventProjection;
import org.example.calendar.event.day.projection.mapper.DayEventProjectionRowMapper;
import org.example.calendar.event.slot.day.projection.DayEventSlotProjection;
import org.example.calendar.utils.EventUtils;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.sql.Date;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DayEventRepository {
    private final JdbcClient jdbcClient;

    public void create(DayEvent event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcClient.sql("""
                        INSERT INTO day_events(organizer_id, start_date, end_date, recurrence_frequency, recurrence_step, weekly_recurrence_days, monthly_recurrence_type, recurrence_duration, recurrence_end_date, number_of_occurrences)
                        VALUES (:organizerId, :startDate, :endDate, :recurrenceFrequency::recurrence_frequency, :recurrenceStep, :weeklyRecurrenceDays, :monthlyRecurrenceType::monthly_recurrence_type, :recurrenceDuration::recurrence_duration, :recurrenceEndDate, :numberOfOccurrences)
                        """)
                .param("organizerId", event.getOrganizerId())
                // java Date to sql Date
                .param("startDate", Date.valueOf(event.getStartDate()))
                .param("endDate", Date.valueOf(event.getEndDate()))
                .param("recurrenceFrequency", event.getRecurrenceFrequency().name())
                .param("recurrenceStep", event.getRecurrenceStep())
                .param("weeklyRecurrenceDays", EventUtils.convertToCsv(event.getWeeklyRecurrenceDays()))
                .param("monthlyRecurrenceType", event.getMonthlyRecurrenceType() == null ? null : event.getMonthlyRecurrenceType().name())
                .param("recurrenceDuration", event.getRecurrenceDuration() == null ? null : event.getRecurrenceDuration().name())
                .param("recurrenceEndDate", event.getRecurrenceEndDate() == null ? null : Date.valueOf(event.getRecurrenceEndDate()))
                .param("numberOfOccurrences", event.getNumberOfOccurrences())
                // If we don't pass the column that will hold the auto-generated key, all the columns will be returned and
                // when we try to access it, we would get InvalidDataAccessApiUsageException: The getKey method should only be used when a single key is returned. The current key entry contains multiple keys: [{id=5, email=sylvester.schneider@yahoo.com, username=emeline.haley, password=$2a$10$Fb0ngWuoUI.U8Q/K2.ZkZebeTM1TocDYz3Utttoe4Y80F4IhVALGm}]
                .update(keyHolder, "id");

        event.setId(keyHolder.getKeyAs(UUID.class));
    }

    /*
        We update only the properties that were changed by keeping track of the record as it was returned from the database
        and compare it with its modified version
     */
    void update(DayEvent original, DayEvent modified) {
        StringBuilder sql = new StringBuilder("UPDATE day_events SET ");
        Map<String, Object> params = new HashMap<>();
        EventUtils.updateDateProperties(sql, original.getStartDate(), modified.getStartDate(), original.getEndDate(), modified.getEndDate(), params);
        EventUtils.updateEventFrequencyProperties(sql, original, modified, params);

        // If any properties were updated, execute the update
        if (!params.isEmpty()) {
            // Remove the last ", "
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = :eventId");
            params.put("eventId", original.getId());
            this.jdbcClient.sql(sql.toString())
                    .params(params)
                    .update();
        }
    }

    /*
        de.id AS day_event_id,
        de.organizer_id,
        de.start_date AS day_event_start_date,
        de.end_date AS day_event_end_date,
        de.recurrence_frequency,
        de.recurrence_step,
        de.weekly_recurrence_days,
        de.monthly_recurrence_type,
        de.recurrence_duration,
        de.recurrence_end_date,
        de.number_of_occurrences,
        des.id AS day_event_slot_id,
        des.title,
        des.location,
        des.description,
        des.start_date AS day_event_slot_start_date,
        des.end_date AS day_event_slot_end_date,
        ge.email AS guest_email

        We need to give an alias to attributes with the same name, so we can have them all as part of our result set,
        otherwise it would be ambiguous. For example, as we fetch both day event and day event slots, we will have 2
        attributes with the same name, start_date and id. We need to add an alias with AS.

        Our query is going to return multiple rows for the same event if it has more than 1 event slot and multiple rows
        for the same event slot if it has multiple guests. Our DayEventProjectionRowMapper() will map each row correctly,
        to our projection.

        For example, an event with 2 event slots where each slot has 2 guest emails will give us a result set of size 4.
            1st row: event information, first event slot information with first guest email
            2nd row: event information, first event slot information with second guest email
            3rd row: event information, second event slot information with first guest email
            4th row: event information, second event slot information with second guest email
        This is standard behaviour of dbs. Now after using the row mapper each row will be a projection. What we want
        to do next is convert them to a single event where it will have a list of event slots and each event slot will
        have each own list of guests. We do the conversion in aggregateResults()
     */
    Optional<DayEventProjection> findByEventAndUserId(UUID eventId, Long userId) {
        List<DayEventProjection> results = this.jdbcClient.sql("""
                            SELECT
                                de.id AS event_id,
                                de.start_date,
                                de.end_date,
                                de.recurrence_frequency,
                                de.recurrence_step,
                                de.weekly_recurrence_days,
                                de.monthly_recurrence_type,
                                de.recurrence_duration,
                                de.recurrence_end_date,
                                de.number_of_occurrences,
                                des.id AS event_slot_id,
                                des.title,
                                des.location,
                                des.description,
                                ge.email
                            FROM day_events de
                            JOIN day_event_slots des ON de.id = des.event_id
                            LEFT JOIN day_event_slot_guest_emails ge ON des.id = ge.event_slot_id
                            WHERE de.id = :eventId AND de.organizer_id = :userId
                        """)
                .param("eventId", eventId)
                .param("userId", userId)
                .query(new DayEventProjectionRowMapper())
                .list();

        return aggregateResults(results);
    }

    // Slots will be deleted by ON DELETE CASCADE
    int deleteByEventAndUserId(UUID eventId, Long userId) {
        return this.jdbcClient.sql("""
                            DELETE FROM day_events
                            WHERE id = :eventId AND organizer_id = :userId
                        """)
                .param("eventId", eventId)
                .param("userId", userId)
                .update();
    }

    private Optional<DayEventProjection> aggregateResults(List<DayEventProjection> resultSet) {
        if (resultSet.isEmpty()) {
            return Optional.empty();
        }

        Map<UUID, DayEventProjection> eventMap = new HashMap<>();
        Map<UUID, DayEventSlotProjection> slotMap = new LinkedHashMap<>();

        for (DayEventProjection event : resultSet) {
            DayEventProjection existingEvent = eventMap.get(event.getId());

            if (existingEvent == null) {
                eventMap.put(event.getId(), event);
                slotMap.put(event.getEventSlots().get(0).getId(), event.getEventSlots().get(0));
            } else {
                DayEventSlotProjection slotProjection = event.getEventSlots().get(0);
                DayEventSlotProjection existingSlot = slotMap.get(slotProjection.getId());

                if (existingSlot == null) {
                    slotMap.put(slotProjection.getId(), slotProjection);
                    existingEvent.getEventSlots().add(slotProjection);
                } else {
                    existingSlot.getGuestEmails().add(slotProjection.getGuestEmails().iterator().next());
                }
            }
        }
        return eventMap.values().stream().findFirst();
    }
}
