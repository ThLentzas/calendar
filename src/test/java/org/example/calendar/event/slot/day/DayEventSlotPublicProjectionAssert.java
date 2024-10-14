package org.example.calendar.event.slot.day;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.api.AbstractAssert;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;

public class DayEventSlotPublicProjectionAssert extends AbstractAssert<DayEventSlotPublicProjectionAssert, DayEventSlotPublicProjection> {

    public DayEventSlotPublicProjectionAssert(DayEventSlotPublicProjection projection) {
        super(projection, DayEventSlotPublicProjectionAssert.class);
    }

    public static DayEventSlotPublicProjectionAssert assertThat(DayEventSlotPublicProjection actual) {
        return new DayEventSlotPublicProjectionAssert(actual);
    }

    public DayEventSlotPublicProjectionAssert hasStartDate(LocalDate startDate) {
        isNotNull();

        if (!actual.getStartDate().isEqual(startDate)) {
            failWithMessage("Expected startDate to be <%s> but was <%s>", startDate, actual.getStartDate());
        }
        return this;
    }

    public DayEventSlotPublicProjectionAssert hasEndDate(LocalDate endDate) {
        isNotNull();

        if (!actual.getEndDate().isEqual(endDate)) {
            failWithMessage("Expected start date to be <%s> but was <%s>", endDate, actual.getEndDate());
        }
        return this;
    }

    public DayEventSlotPublicProjectionAssert hasTitle(String title) {
        isNotNull();

        if (!Objects.equals(actual.getTitle(), title)) {
            failWithMessage("Expected title to be <%s> but was <%s>", title, actual.getTitle());
        }
        return this;
    }

    public DayEventSlotPublicProjectionAssert hasLocation(String location) {
        isNotNull();

        if (!Objects.equals(actual.getLocation(), location)) {
            failWithMessage("Expected location to be <%s> but was <%s>", location, actual.getLocation());
        }
        return this;
    }

    public DayEventSlotPublicProjectionAssert hasDescription(String description) {
        isNotNull();

        if (!Objects.equals(actual.getDescription(), description)) {
            failWithMessage("Expected description to be <%s> but was <%s>", description, actual.getDescription());
        }
        return this;
    }

    public DayEventSlotPublicProjectionAssert hasGuests(Set<String> guestEmails) {
        isNotNull();
        if (!actual.getGuestEmails().equals(guestEmails)) {
            failWithMessage("Expected guestEmails to be <%s> but was <%s>", guestEmails, actual.getGuestEmails());
        }

        return this;
    }

    public DayEventSlotPublicProjectionAssert hasEventId(UUID eventId) {
        isNotNull();

        if (!actual.getEventId().equals(eventId)) {
            failWithMessage("Expected eventId to be <%s> but was <%s>", eventId, actual.getEventId());
        }
        return this;
    }
}
