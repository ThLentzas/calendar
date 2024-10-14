package org.example.calendar.event.slot.time.projection.mapper;

import org.example.calendar.event.slot.time.projection.TimeEventSlotReminderProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.TreeSet;
import java.util.UUID;

public class TimeEventSlotReminderProjectionRowMapper implements RowMapper<TimeEventSlotReminderProjection> {

    @Override
    public TimeEventSlotReminderProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        TimeEventSlotReminderProjection projection = TimeEventSlotReminderProjection.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .startTime(resultSet.getTimestamp("start_time").toLocalDateTime())
                .endTime(resultSet.getTimestamp("end_time").toLocalDateTime())
                .organizerEmail(resultSet.getString("organizer_email"))
                .organizerUsername(resultSet.getString("username"))
                .title(resultSet.getString("title"))
                .guestEmails(new TreeSet<>())
                .build();

        if (resultSet.getString("guest_email") != null) {
            projection.getGuestEmails().add(resultSet.getString("guest_email"));
        }

        return projection;
    }
}
