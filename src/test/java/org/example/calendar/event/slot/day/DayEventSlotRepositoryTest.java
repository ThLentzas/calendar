package org.example.calendar.event.slot.day;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.event.slot.day.projection.DayEventSlotReminderProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/*
    Every other method of the repository is tested via the service expect this one. This one is used by the Notification
    service for scheduled tasks.
 */
@Import(DayEventSlotRepository.class)
@Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
class DayEventSlotRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private DayEventSlotRepository underTest;

    @Test
    void shouldFindEventSlotsByStartDate() {
        List<DayEventSlotReminderProjection> projections = this.underTest.findByStartDate(LocalDate.parse("2024-10-12"));

        assertThat(projections.get(0).getId()).isEqualTo(UUID.fromString("e2985eda-5c5a-40a0-851e-6dc088081afa"));
        assertThat(projections.get(0).getStartDate().isEqual(LocalDate.parse("2024-10-12"))).isTrue();
        assertThat(projections.get(0).getTitle()).isEqualTo("Event title");
        assertThat(projections.get(0).getOrganizerEmail()).isEqualTo("ericka.ankunding@hotmail.com");
        assertThat(projections.get(0).getOrganizerUsername()).isEqualTo("clement.gulgowski");
        assertThat(projections.get(0).getGuestEmails()).isEmpty();
    }
}
