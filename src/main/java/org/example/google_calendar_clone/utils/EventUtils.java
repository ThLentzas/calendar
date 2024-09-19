package org.example.google_calendar_clone.utils;

import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotReminderRequest;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotReminderRequest;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ConflictException;

import java.util.Set;
import java.util.stream.Collectors;

public final class EventUtils {

    private EventUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("EventUtils is a utility class and cannot be instantiated");
    }

    public static Set<String> processGuestEmails(User user, InviteGuestsRequest guestsRequest, Set<String> guestEmails) {
        if (guestsRequest.guestEmails().contains(user.getEmail())) {
            throw new ConflictException("Organizer of the event can't be added as guest");
        }

        // We filter out emails that don't contain @ and exclude emails that are already invited
        return guestsRequest.guestEmails().stream()
                .filter(email -> email.contains("@"))
                .filter(email -> !guestEmails.contains(email))
                .collect(Collectors.toSet());
    }

    public static DayEventSlotReminderRequest mapToReminderRequest(DayEventSlot eventSlot) {
        return DayEventSlotReminderRequest.builder()
                .id(eventSlot.getId())
                .eventName(eventSlot.getName())
                .startDate(eventSlot.getStartDate())
                .organizer(eventSlot.getDayEvent().getUser())
                .guestEmails(eventSlot.getGuestEmails())
                .build();
    }

    public static TimeEventSlotReminderRequest mapToReminderRequest(TimeEventSlot eventSlot) {
        return TimeEventSlotReminderRequest.builder()
                .id(eventSlot.getId())
                .eventName(eventSlot.getName())
                .startTime(eventSlot.getStartTime())
                .endTime(eventSlot.getEndTime())
                .organizer(eventSlot.getTimeEvent().getUser())
                .guestEmails(eventSlot.getGuestEmails())
                .build();
    }
}
