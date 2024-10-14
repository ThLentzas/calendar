package org.example.calendar.event.slot.time.projection.mapper;

import org.example.calendar.event.slot.time.projection.TimeEventSlotProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.TreeSet;
import java.util.UUID;

public class TimeEventSlotProjectionRowMapper implements RowMapper<TimeEventSlotProjection> {

    @Override
    public TimeEventSlotProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        TimeEventSlotProjection slotProjection = TimeEventSlotProjection.builder()
                .id(UUID.fromString(resultSet.getString("event_slot_id")))
                .starTime(resultSet.getTimestamp("start_time").toLocalDateTime())
                .startTimeZoneId(ZoneId.of(resultSet.getString("start_time_zone_id")))
                .endTime(resultSet.getTimestamp("end_time").toLocalDateTime())
                .endTimeZoneId(ZoneId.of(resultSet.getString("end_time_zone_id")))
                .title(resultSet.getString("title"))
                .description(resultSet.getString("description"))
                .location(resultSet.getString("location"))
                .guestEmails(new TreeSet<>())
                .build();

        if (resultSet.getString("email") != null) {
            slotProjection.getGuestEmails().add(resultSet.getString("email"));
        }

        return slotProjection;
    }
}
