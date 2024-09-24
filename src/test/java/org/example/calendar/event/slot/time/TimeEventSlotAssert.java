package org.example.calendar.event.slot.time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.example.calendar.entity.TimeEvent;
import org.example.calendar.entity.TimeEventSlot;

import org.assertj.core.api.AbstractAssert;

class TimeEventSlotAssert extends AbstractAssert<TimeEventSlotAssert, TimeEventSlot> {

    TimeEventSlotAssert(TimeEventSlot timeEventSlot) {
        super(timeEventSlot, TimeEventSlotAssert.class);
    }

    static TimeEventSlotAssert assertThat(TimeEventSlot actual) {
        return new TimeEventSlotAssert(actual);
    }

    TimeEventSlotAssert hasStartTime(LocalDateTime startTime) {
        isNotNull();

        if (!actual.getStartTime().equals(startTime)) {
            failWithMessage("Expected startTime to be <%s> but was <%s>", startTime, actual.getStartTime());
        }
        return this;
    }

    TimeEventSlotAssert hasEndTime(LocalDateTime endTime) {
        isNotNull();

        if (!actual.getEndTime().equals(endTime)) {
            failWithMessage("Expected endTime to be <%s> but was <%s>", endTime, actual.getEndTime());
        }
        return this;
    }

    TimeEventSlotAssert hasStartTimeZoneId(ZoneId startTimeZoneId) {
        isNotNull();

        if (!actual.getStartTimeZoneId().equals(startTimeZoneId)) {
            failWithMessage("Expected startTimeZoneId to be <%s> but was <%s>", startTimeZoneId, actual.getStartTimeZoneId());
        }
        return this;
    }

    TimeEventSlotAssert hasEndTimeZoneId(ZoneId endTimeZoneId) {
        isNotNull();

        if (!actual.getEndTimeZoneId().equals(endTimeZoneId)) {
            failWithMessage("Expected endTimeZoneId to be <%s> but was <%s>", endTimeZoneId, actual.getEndTimeZoneId());
        }
        return this;
    }

    TimeEventSlotAssert hasTitle(String title) {
        isNotNull();

        if (!actual.getTitle().equals(title)) {
            failWithMessage("Expected title to be <%s> but was <%s>", title, actual.getTitle());
        }
        return this;
    }

    TimeEventSlotAssert hasLocation(String location) {
        isNotNull();

        if (!actual.getLocation().equals(location)) {
            failWithMessage("Expected location to be <%s> but was <%s>", location, actual.getLocation());
        }
        return this;
    }

    TimeEventSlotAssert hasDescription(String description) {
        isNotNull();

        if (!actual.getDescription().equals(description)) {
            failWithMessage("Expected description to be <%s> but was <%s>", description, actual.getDescription());
        }
        return this;
    }

    TimeEventSlotAssert hasGuests(Set<String> guestEmails) {
        isNotNull();
        if (!actual.getGuestEmails().equals(guestEmails)) {
            failWithMessage("Expected guestEmails to be <%s> but was <%s>", guestEmails, actual.getGuestEmails());
        }

        return this;
    }

    TimeEventSlotAssert hasTimeEvent(TimeEvent timeEvent) {
        isNotNull();

        if (!actual.getTimeEvent().equals(timeEvent)) {
            failWithMessage("Expected dayEvent to be <%s> but was <%s>", timeEvent, actual.getTimeEvent());
        }
        return this;
    }
}

