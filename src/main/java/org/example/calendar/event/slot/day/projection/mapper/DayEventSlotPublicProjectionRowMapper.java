package org.example.calendar.event.slot.day.projection.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.UUID;

import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.springframework.jdbc.core.RowMapper;

public class DayEventSlotPublicProjectionRowMapper implements RowMapper<DayEventSlotPublicProjection> {

    // We don't have to handle the result set ourselves: while(resultSet.next()). JdbcClient will call our mapper for each
    // row in the result set
    @Override
    public DayEventSlotPublicProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        DayEventSlotPublicProjection eventSlot = DayEventSlotPublicProjection.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .startDate(resultSet.getDate("start_date").toLocalDate())
                .endDate(resultSet.getDate("end_date").toLocalDate())
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
