package org.example.google_calendar_clone.utils;

import jakarta.validation.ConstraintValidatorContext;
import org.example.google_calendar_clone.calendar.event.AbstractEvent;
import org.example.google_calendar_clone.calendar.event.day.dto.UpdateDayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotReminderRequest;
import org.example.google_calendar_clone.calendar.event.dto.AbstractEventRequest;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.UpdateTimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotReminderRequest;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ConflictException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class EventUtils {

    private EventUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("EventUtils is a utility class and cannot be instantiated");
    }

    public static boolean hasValidFrequencyProperties(AbstractEventRequest eventRequest, ConstraintValidatorContext context) {
        /*
            When the frequency is NEVER, we can ignore the values of the remaining fields(set them to null) and process
            the request. We also reduce the total checks.
         */
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.NEVER)) {
            eventRequest.setRepetitionStep(null);
            eventRequest.setWeeklyRecurrenceDays(null);
            eventRequest.setMonthlyRepetitionType(null);
            eventRequest.setRepetitionDuration(null);
            eventRequest.setRepetitionEndDate(null);
            eventRequest.setRepetitionOccurrences(null);
            return true;
        }

        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY)
                && (eventRequest.getWeeklyRecurrenceDays() == null || eventRequest.getWeeklyRecurrenceDays().isEmpty())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide at least one day of the week for weekly repeating events")
                    .addConstraintViolation();
            return false;
        }

        if (!eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY)
                && eventRequest.getWeeklyRecurrenceDays() != null
                && !eventRequest.getWeeklyRecurrenceDays().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Weekly recurrence days are only valid for weekly repeating " +
                            "events")
                    .addConstraintViolation();
            return false;
        }

        // https://stackoverflow.com/questions/19825563/custom-validator-message-throwing-exception-in-implementation-of-constraintvali/19833921#19833921
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY)
                && eventRequest.getMonthlyRepetitionType() == null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a monthly repetition type for monthly " +
                            "repeating events")
                    .addConstraintViolation();
            return false;
        }

        if (!eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY)
                && eventRequest.getMonthlyRepetitionType() != null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Monthly repetition types are only valid for monthly " +
                            "repeating events")
                    .addConstraintViolation();
            return false;
        }

        // At this point getMonthlyType() will always be null
        if (eventRequest.getRepetitionDuration() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify an end date or a number of repetitions for" +
                            " repeating events")
                    .addConstraintViolation();
            return false;
        }

        /*
            Similar to NEVER, we don't have to check for repetition end date or repetition count is the repetition is
            FOREVER.
         */
        if (eventRequest.getRepetitionDuration().equals(RepetitionDuration.FOREVER)) {
            eventRequest.setRepetitionEndDate(null);
            eventRequest.setRepetitionOccurrences(null);
            return true;
        }

        // Duration is UNTIL_DATE and repetitionEndDate is null
        if (eventRequest.getRepetitionDuration().equals(RepetitionDuration.UNTIL_DATE)
                && eventRequest.getRepetitionEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The repetition end date is required when repetition " +
                            "duration is set to until a certain date")
                    .addConstraintViolation();
            return false;
        }

        // Duration is N_REPETITIONS and repetitionCount is null
        if (eventRequest.getRepetitionDuration().equals(RepetitionDuration.N_REPETITIONS)
                && (eventRequest.getRepetitionOccurrences() == null || eventRequest.getRepetitionOccurrences() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The number of repetitions is required when repetition " +
                            "duration is set to a certain number of repetitions")
                    .addConstraintViolation();
            return false;
        }

        // Both end date and repetition count were provided
        if (eventRequest.getRepetitionEndDate() != null && eventRequest.getRepetitionOccurrences() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify either a repetition end date or a number of " +
                            "repetitions. Not both")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    public static void setFrequencyDetails(AbstractEventRequest eventRequest, AbstractEvent event) {
        event.setRepetitionFrequency(eventRequest.getRepetitionFrequency());
        event.setRepetitionStep(eventRequest.getRepetitionStep());
        event.setWeeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays());
        event.setMonthlyRepetitionType(eventRequest.getMonthlyRepetitionType());
        event.setRepetitionDuration(eventRequest.getRepetitionDuration());
        event.setRepetitionEndDate(eventRequest.getRepetitionEndDate());
        event.setRepetitionOccurrences(eventRequest.getRepetitionOccurrences());
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
                .eventName(eventSlot.getTitle())
                .startDate(eventSlot.getStartDate())
                .organizer(eventSlot.getDayEvent().getUser())
                .guestEmails(eventSlot.getGuestEmails())
                .build();
    }

    public static TimeEventSlotReminderRequest mapToReminderRequest(TimeEventSlot eventSlot) {
        return TimeEventSlotReminderRequest.builder()
                .id(eventSlot.getId())
                .eventName(eventSlot.getTitle())
                .startTime(eventSlot.getStartTime())
                .endTime(eventSlot.getEndTime())
                .organizer(eventSlot.getTimeEvent().getUser())
                .guestEmails(eventSlot.getGuestEmails())
                .build();
    }

    public static boolean hasSameFrequencyDetails(AbstractEventRequest eventRequest, AbstractEvent event) {
        return Objects.equals(eventRequest.getRepetitionFrequency(), event.getRepetitionFrequency())
                && Objects.equals(eventRequest.getRepetitionStep(), event.getRepetitionStep())
                && Objects.equals(eventRequest.getWeeklyRecurrenceDays(), event.getWeeklyRecurrenceDays())
                && Objects.equals(eventRequest.getMonthlyRepetitionType(), event.getMonthlyRepetitionType())
                && Objects.equals(eventRequest.getRepetitionDuration(), event.getRepetitionDuration())
                && Objects.equals(eventRequest.getRepetitionEndDate(), event.getRepetitionEndDate())
                && Objects.equals(eventRequest.getRepetitionOccurrences(), event.getRepetitionOccurrences());
    }

    public static boolean emptyUpdateRequestProperties(UpdateTimeEventRequest eventRequest) {
        return eventRequest.getStartTime() == null
                && eventRequest.getEndTime() == null
                && eventRequest.getStartTimeZoneId() == null
                && eventRequest.getEndTimeZoneId() == null
                && emptyRequestProperties(eventRequest);
    }

    public static boolean emptyUpdateRequestProperties(UpdateDayEventRequest eventRequest) {
        return eventRequest.getStartDate() == null
                && eventRequest.getEndDate() == null
                && emptyRequestProperties(eventRequest);
    }

    // Checks the common fields
    private static boolean emptyRequestProperties(AbstractEventRequest eventRequest) {
        return ((eventRequest.getTitle() == null || eventRequest.getTitle().isBlank())
                && (eventRequest.getLocation() == null || eventRequest.getLocation().isBlank())
                && (eventRequest.getDescription() == null || eventRequest.getDescription().isBlank())
                && (eventRequest.getGuestEmails() == null || eventRequest.getGuestEmails().isEmpty())
                && eventRequest.getRepetitionFrequency() == null
                && (eventRequest.getRepetitionStep() == null || eventRequest.getRepetitionStep() == 0)
                && (eventRequest.getWeeklyRecurrenceDays() == null || eventRequest.getWeeklyRecurrenceDays().isEmpty())
                && eventRequest.getMonthlyRepetitionType() == null
                && eventRequest.getRepetitionDuration() == null
                && eventRequest.getRepetitionEndDate() == null
                && (eventRequest.getRepetitionOccurrences() == null || eventRequest.getRepetitionOccurrences() == 0));
    }
}
