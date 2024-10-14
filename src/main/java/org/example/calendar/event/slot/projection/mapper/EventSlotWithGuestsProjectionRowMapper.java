package org.example.calendar.event.slot.projection.mapper;

import org.example.calendar.event.slot.projection.EventSlotWithGuestsProjection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.UUID;

public class EventSlotWithGuestsProjectionRowMapper implements RowMapper<EventSlotWithGuestsProjection> {

    @Override
    public EventSlotWithGuestsProjection mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        EventSlotWithGuestsProjection slotProjection = EventSlotWithGuestsProjection.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .guestEmails(new TreeSet<>())
                .build();

        if (resultSet.getString("email") != null) {
            slotProjection.getGuestEmails().add(resultSet.getString("email"));
        }

        return slotProjection;
    }
}
