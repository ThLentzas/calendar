package org.example.calendar.event.time.projection.mapper;

import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.slot.time.projection.TimeEventSlotProjection;
import org.example.calendar.event.slot.time.projection.mapper.TimeEventSlotProjectionRowMapper;
import org.example.calendar.event.time.projection.TimeEventProjection;
import org.example.calendar.utils.EventUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/*
   For recurrence step and number of occurrences if they are null at the db it would set the value of 0. Also for the
   weekly recurrence days, convertFromCsv returns an empty set when the db csv string is null, and we return in that case
   an empty set. If we don't handle those cases, when calling EventUtils.hasSameFrequencyProperties() would fail because
   it would compare null with 0 and empty set with null.
 */
public class TimeEventProjectionRowMapper implements RowMapper<TimeEventProjection> {
    private final TimeEventSlotProjectionRowMapper eventSlotProjectionRowMapper = new TimeEventSlotProjectionRowMapper();

    @Override
    public TimeEventProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Set<DayOfWeek> weeklyRecurrenceDays = EventUtils.convertFromCsv(resultSet.getString("weekly_recurrence_days"));

        // We don't have to handle the result set ourselves: while(resultSet.next()). JdbcClient will call our mapper for each
        // row in the result set
        TimeEventProjection eventProjection = TimeEventProjection.builder()
                .id(UUID.fromString(resultSet.getString("event_id")))
                .starTime(resultSet.getTimestamp("start_time").toLocalDateTime())
                .startTimeZoneId(ZoneId.of(resultSet.getString("start_time_zone_id")))
                .endTime(resultSet.getTimestamp("end_time").toLocalDateTime())
                .endTimeZoneId(ZoneId.of(resultSet.getString("end_time_zone_id")))
                .recurrenceFrequency(RecurrenceFrequency.valueOf(resultSet.getString("recurrence_frequency")))
                .recurrenceStep(resultSet.getInt("recurrence_step") == 0 ? null : resultSet.getInt("recurrence_step"))
                .weeklyRecurrenceDays(weeklyRecurrenceDays.isEmpty() ? null : weeklyRecurrenceDays)
                .monthlyRecurrenceType(resultSet.getString("monthly_recurrence_type") == null ? null : MonthlyRecurrenceType.valueOf(resultSet.getString("monthly_recurrence_type")))
                .recurrenceDuration(resultSet.getString("recurrence_duration") == null ? null : RecurrenceDuration.valueOf(resultSet.getString("recurrence_duration")))
                .recurrenceEndDate(resultSet.getDate("recurrence_end_date") == null ? null : resultSet.getDate("recurrence_end_date").toLocalDate())
                .numberOfOccurrences(resultSet.getInt("number_of_occurrences") == 0 ? null : resultSet.getInt("number_of_occurrences"))
                .eventSlots(new ArrayList<>())
                .build();

        if (resultSet.getString("event_slot_id") != null) {
            TimeEventSlotProjection slotProjection = this.eventSlotProjectionRowMapper.mapRow(resultSet, rowNum);
            eventProjection.getEventSlots().add(slotProjection);
        }
        return eventProjection;
    }
}
