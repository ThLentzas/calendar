package org.example.calendar.event.slot.time;

import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.event.slot.time.projection.TimeEventSlotReminderProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/*
    Every other method of the repository is tested via the service expect this one. This one is used by the Notification
    service for scheduled tasks.
 */
@Import(TimeEventSlotRepository.class)
@Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
class TimeEventSlotRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private TimeEventSlotRepository underTest;

    @Test
    void shouldFindEventSlotsByStartTime() {
        List<TimeEventSlotReminderProjection> projections = this.underTest.findByStartTime(LocalDateTime.parse("2024-10-29T09:00:00"));

        assertThat(projections.get(0).getId()).isEqualTo(UUID.fromString("f8020ab5-1bc8-4b45-9d77-1a3859c264dd"));
        assertThat(projections.get(0).getStartTime().isEqual(LocalDateTime.parse("2024-10-29T09:00:00"))).isTrue();
        assertThat(projections.get(0).getEndTime().isEqual(LocalDateTime.parse("2024-10-29T14:00:00"))).isTrue();
        assertThat(projections.get(0).getTitle()).isEqualTo("Event title");
        assertThat(projections.get(0).getOrganizerEmail()).isEqualTo("joshua.wolf@hotmail.com");
        assertThat(projections.get(0).getOrganizerUsername()).isEqualTo("kris.hudson");
        assertThat(projections.get(0).getGuestEmails()).hasSize(1)
                .contains("ericka.ankunding@hotmail.com");
    }
}
