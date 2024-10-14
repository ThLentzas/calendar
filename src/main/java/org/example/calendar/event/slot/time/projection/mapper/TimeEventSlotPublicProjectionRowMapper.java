package org.example.calendar.event.slot.time.projection.mapper;

import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.TreeSet;
import java.util.UUID;

public class TimeEventSlotPublicProjectionRowMapper implements RowMapper<TimeEventSlotPublicProjection> {

    @Override
    public TimeEventSlotPublicProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        TimeEventSlotPublicProjection eventSlot = TimeEventSlotPublicProjection.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .startTime(resultSet.getTimestamp("start_time").toLocalDateTime())
                .startTimeZoneId(ZoneId.of(resultSet.getString("start_time_zone_id")))
                .endTime(resultSet.getTimestamp("end_time").toLocalDateTime())
                .endTimeZoneId(ZoneId.of(resultSet.getString("end_time_zone_id")))
                .title(resultSet.getString("title"))
                .description(resultSet.getString("description"))
                .location(resultSet.getString("location"))
                .organizer(resultSet.getString("username"))
                .eventId(UUID.fromString(resultSet.getString("event_id")))
                .guestEmails(new TreeSet<>())
                .build();

        if (resultSet.getString("email") != null) {
            eventSlot.getGuestEmails().add(resultSet.getString("email"));
        }
        return eventSlot;
    }
}
