package org.example.calendar.event.slot.day.projection.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.UUID;

import org.example.calendar.event.slot.day.projection.DayEventSlotReminderProjection;
import org.springframework.jdbc.core.RowMapper;

public class DayEventSlotReminderProjectionRowMapper implements RowMapper<DayEventSlotReminderProjection> {

    // We don't have to handle the result set ourselves: while(resultSet.next()). JdbcClient will call our mapper for each
    // row in the result set
    @Override
    public DayEventSlotReminderProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        DayEventSlotReminderProjection projection = DayEventSlotReminderProjection.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .startDate(resultSet.getDate("start_date").toLocalDate())
                .title(resultSet.getString("title"))
                .organizerEmail(resultSet.getString("organizer_email"))
                .organizerUsername(resultSet.getString("username"))
                .guestEmails(new TreeSet<>())
                .build();

        if (resultSet.getString("guest_email") != null) {
            projection.getGuestEmails().add(resultSet.getString("guest_email"));
        }

        return projection;
    }
}
