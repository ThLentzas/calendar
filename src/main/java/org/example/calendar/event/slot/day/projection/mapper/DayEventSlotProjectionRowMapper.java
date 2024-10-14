package org.example.calendar.event.slot.day.projection.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.UUID;

import org.example.calendar.event.slot.day.projection.DayEventSlotProjection;
import org.springframework.jdbc.core.RowMapper;

public class DayEventSlotProjectionRowMapper implements RowMapper<DayEventSlotProjection> {

    // We don't have to handle the result set ourselves: while(resultSet.next()). JdbcClient will call our mapper for each
    // row in the result set
    @Override
    public DayEventSlotProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        DayEventSlotProjection slotProjection = DayEventSlotProjection.builder()
                .id(UUID.fromString(resultSet.getString("event_slot_id")))
                .startDate(resultSet.getDate("start_date").toLocalDate())
                .endDate(resultSet.getDate("end_date").toLocalDate())
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
