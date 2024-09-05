package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.assertj.core.api.AbstractAssert;

import java.time.LocalDate;
import java.util.Set;

class DayEventSlotAssert extends AbstractAssert<DayEventSlotAssert, DayEventSlot> {

    DayEventSlotAssert(DayEventSlot dayEventSlot) {
        super(dayEventSlot, DayEventSlotAssert.class);
    }

    static DayEventSlotAssert assertThat(DayEventSlot actual) {
        return new DayEventSlotAssert(actual);
    }

    DayEventSlotAssert hasStartDate(LocalDate startDate) {
        isNotNull();

        if(!actual.getStartDate().equals(startDate)) {
            failWithMessage("Expected startDate to be <%s> but was <%s>", startDate, actual.getStartDate());
        }
        return this;
    }

    DayEventSlotAssert hasEndDate(LocalDate endDate) {
        isNotNull();

        if(!actual.getEndDate().equals(endDate)) {
            failWithMessage("Expected start date to be <%s> but was <%s>", endDate, actual.getEndDate());
        }
        return this;
    }

    DayEventSlotAssert hasName(String eventName) {
        isNotNull();

        if(!actual.getName().equals(eventName)) {
            failWithMessage("Expected eventName to be <%s> but was <%s>", eventName, actual.getName());
        }
        return this;
    }

    DayEventSlotAssert hasLocation(String location) {
        isNotNull();

        if(!actual.getLocation().equals(location)) {
            failWithMessage("Expected location to be <%s> but was <%s>", location, actual.getLocation());
        }
        return this;
    }

    DayEventSlotAssert hasDescription(String description) {
        isNotNull();

        if(!actual.getDescription().equals(description)) {
            failWithMessage("Expected description to be <%s> but was <%s>", description, actual.getDescription());
        }
        return this;
    }

    DayEventSlotAssert hasGuests(Set<String> guestEmails) {
        isNotNull();
        if(!actual.getGuestEmails().equals(guestEmails)) {
            failWithMessage("Expected guestEmails to be <%s> but was <%s>", guestEmails, actual.getGuestEmails());
        }

        return this;
    }

    DayEventSlotAssert hasDayEvent(DayEvent dayEvent) {
        isNotNull();

        if(!actual.getDayEvent().equals(dayEvent)) {
            failWithMessage("Expected dayEvent to be <%s> but was <%s>", dayEvent, actual.getDayEvent());
        }
        return this;
    }
}
