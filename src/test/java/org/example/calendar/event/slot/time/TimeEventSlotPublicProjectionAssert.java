package org.example.calendar.event.slot.time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.api.AbstractAssert;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;

public class TimeEventSlotPublicProjectionAssert extends AbstractAssert<TimeEventSlotPublicProjectionAssert, TimeEventSlotPublicProjection> {

    public TimeEventSlotPublicProjectionAssert(TimeEventSlotPublicProjection projection) {
        super(projection, TimeEventSlotPublicProjectionAssert.class);
    }

    public static TimeEventSlotPublicProjectionAssert assertThat(TimeEventSlotPublicProjection actual) {
        return new TimeEventSlotPublicProjectionAssert(actual);
    }

    public TimeEventSlotPublicProjectionAssert hasStartTime(LocalDateTime startTime) {
        isNotNull();

        if (!actual.getStartTime().equals(startTime)) {
            failWithMessage("Expected startTime to be <%s> but was <%s>", startTime, actual.getStartTime());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasStartTimeZoneId(ZoneId startTimeZoneId) {
        isNotNull();

        if (!actual.getStartTimeZoneId().equals(startTimeZoneId)) {
            failWithMessage("Expected startTimeZoneId to be <%s> but was <%s>", startTimeZoneId, actual.getStartTimeZoneId());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasEndTime(LocalDateTime endTime) {
        isNotNull();

        if (!actual.getEndTime().equals(endTime)) {
            failWithMessage("Expected endTime to be <%s> but was <%s>", endTime, actual.getEndTime());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasEndTimeZoneId(ZoneId endTimeZoneId) {
        isNotNull();

        if (!actual.getEndTimeZoneId().equals(endTimeZoneId)) {
            failWithMessage("Expected endTimeZoneId to be <%s> but was <%s>", endTimeZoneId, actual.getEndTimeZoneId());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasTitle(String title) {
        isNotNull();

        if (!Objects.equals(actual.getTitle(), title)) {
            failWithMessage("Expected title to be <%s> but was <%s>", title, actual.getTitle());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasLocation(String location) {
        isNotNull();

        if (!Objects.equals(actual.getLocation(), location)) {
            failWithMessage("Expected location to be <%s> but was <%s>", location, actual.getLocation());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasDescription(String description) {
        isNotNull();

        if (!Objects.equals(actual.getDescription(), description)) {
            failWithMessage("Expected description to be <%s> but was <%s>", description, actual.getDescription());
        }
        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasGuests(Set<String> guestEmails) {
        isNotNull();
        if (!actual.getGuestEmails().equals(guestEmails)) {
            failWithMessage("Expected guestEmails to be <%s> but was <%s>", guestEmails, actual.getGuestEmails());
        }

        return this;
    }

    public TimeEventSlotPublicProjectionAssert hasEventId(UUID eventId) {
        isNotNull();

        if (!actual.getEventId().equals(eventId)) {
            failWithMessage("Expected eventId to be <%s> but was <%s>", eventId, actual.getEventId());
        }
        return this;
    }
}

