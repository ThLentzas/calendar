package org.example.google_calendar_clone.utils;

import org.example.google_calendar_clone.calendar.event.AbstractEvent;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.dto.AbstractEventRequest;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.slot.AbstractEventSlot;
import org.example.google_calendar_clone.calendar.event.slot.AbstractEventSlotRequest;
import org.example.google_calendar_clone.calendar.event.slot.day.dto.DayEventSlotReminderRequest;
import org.example.google_calendar_clone.calendar.event.slot.day.dto.DayEventSlotRequest;
import org.example.google_calendar_clone.calendar.event.slot.time.dto.TimeEventSlotRequest;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.slot.time.dto.TimeEventSlotReminderRequest;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ConflictException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidatorContext;

public final class EventUtils {

    private EventUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("EventUtils is a utility class and cannot be instantiated");
    }

    // It checks if the DayEventRequest is valid, including date and frequency properties
    public static boolean hasValidEventRequestProperties(DayEventRequest eventRequest,
                                                         ConstraintValidatorContext context) {
        if (!hasValidDateProperties(eventRequest.getStartDate(), eventRequest.getEndDate(), context)) {
            return false;
        }

        if (!hasValidFrequencyProperties(eventRequest, context)) {
            return false;
        }

        // If the date is 2024-09-10 (a Tuesday), the weeklyRecurrenceDays set must contain TUESDAY
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY) && !eventRequest.getWeeklyRecurrenceDays().contains(eventRequest.getStartDate().getDayOfWeek())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date " + eventRequest.getStartDate() + " is a " + eventRequest.getStartDate().getDayOfWeek() + ", but this day is not included in the weekly recurrence days: " + eventRequest.getWeeklyRecurrenceDays())
                    .addConstraintViolation();
            return false;
        }

        /*
            RepetitionOccurrences can only be null at this point. Repetition end date is before the end date
         */
        if (eventRequest.getRepetitionEndDate() != null && DateUtils.isBefore(eventRequest.getRepetitionEndDate(), eventRequest.getEndDate())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Repetition end date must be after end date")
                    .addConstraintViolation();
            return false;
        }

        /*
            If we have a frequency value other than NEVER and the user did not provide a repetition step, meaning how
            often that event will be repeated with the given frequency, we could also default to 1. An example would be,
            when the frequency is DAILY and repetition step is null, we set it to 1. It means that the event will be
            repeated every day until the repetitionEndDate
         */
        if (!eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.NEVER) && (eventRequest.getRepetitionStep() == null || eventRequest.getRepetitionStep() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify how often you want the event to be repeated")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    // It checks if the TimeEventRequest is valid, including date and frequency properties
    public static boolean hasValidEventRequestProperties(TimeEventRequest eventRequest,
                                                         ConstraintValidatorContext context) {
        if (!hasValidDateTimeProperties(eventRequest.getStartTime(), eventRequest.getEndTime(), eventRequest.getStartTimeZoneId(), eventRequest.getEndTimeZoneId(), context)) {
            return false;
        }

        if (!hasValidFrequencyProperties(eventRequest, context)) {
            return false;
        }

        // If the date is 2024-09-10 (a Tuesday), the weeklyRecurrenceDays set must contain TUESDAY
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY) && !eventRequest.getWeeklyRecurrenceDays().contains(eventRequest.getStartTime().getDayOfWeek())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date " + eventRequest.getStartTime() + " is a " + eventRequest.getStartTime().getDayOfWeek() + ", but this day is not included in the weekly recurrence days: " + eventRequest.getWeeklyRecurrenceDays())
                    .addConstraintViolation();
            return false;
        }

        /*
            RepetitionCount can only be null at this point. Repetition end date is before the ending date. When we
            create an event endDate will never be null since the TimeEventRequest has a @NotNull. When we update an event
            since endTime is optional, it can be null, but if it is not we still need to make sure that the
            repetitionEndDate is after endDate.

            We have to extract the Date part of the DateTime before comparing
         */
        if (eventRequest.getRepetitionEndDate() != null && DateUtils.isBefore(eventRequest.getRepetitionEndDate(), LocalDate.from(eventRequest.getEndTime()))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Repetition end date must be after end date")
                    .addConstraintViolation();
            return false;

        }

        /*
            If we have a frequency value other than NEVER and the user did not provide a repetition step, meaning how
            often that event will be repeated with the given frequency, we could also default to 1. An example would be,
            when the frequency is DAILY and repetition step is null, we set it to 1. It means that the event will be
            repeated every day until the repetitionEndDate
         */
        if (!eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.NEVER) && (eventRequest.getRepetitionStep() == null || eventRequest.getRepetitionStep() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify how often you want the event to be repeated")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    public static boolean hasValidDateProperties(LocalDate startDate, LocalDate endDate,
                                                 ConstraintValidatorContext context) {
        /*
            Starting date is before Ending date

            When we create an event startDate and endDate will never be null since the DayEventRequest has a @NotNull.
            When we update an event since startDate and endDate are optional, they can be null, but if they are not we
            still need to make sure that the endDate is after startDate.
         */
        if (DateUtils.isAfter(startDate, endDate)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start date must be before end date")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    public static boolean hasValidDateTimeProperties(LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     ZoneId startTimeZoneId,
                                                     ZoneId endTimeZoneId,
                                                     ConstraintValidatorContext context) {
        // The times must be in the present or future relative to their timezone
        if (DateUtils.futureOrPresent(startTime, startTimeZoneId)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start time must be in the future or present")
                    .addConstraintViolation();
            return false;
        }

        if (DateUtils.futureOrPresent(endTime, endTimeZoneId)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("End time must be in the future or present")
                    .addConstraintViolation();
            return false;
        }

        /*
            Starting time is before Ending time

            We support different timezones for start and end time. We have to consider the following case.
                When user provided start time and end time in different time zones, initially it could be that
                start time < end time without taking into consideration their time-zones, simply comparing the times.
                We have to convert both to UTC and make sure that start time < end time
         */
        if (DateUtils.isAfter(startTime, startTimeZoneId, endTime, endTimeZoneId)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start time must be before end time")
                    .addConstraintViolation();
            return false;
        }

        /*
            A time event can span between 2 days, but no more than 24 hours.

            September 16, 2024, 23:00 (UTC+2, Central European Summer Time)
            September 17, 2024, 03:00 (UTC+4, Dubai Time)

            In UTC the event's duration is from 21:00 - 23:00, it is a 2-hour event that spans in 2 different days, this
            is valid.

            September 16, 2024, 18:00 (UTC+2, Central European Summer Time)
            September 17, 2024, 21:00 (UTC+4, Dubai Time)

            In UTC the event's duration is from September 16, 16:00 - to September 17, 17:00, it is more than 24 hours,
            not a valid Time event

         */
        if (DateUtils.timeZoneAwareDifference(startTime, startTimeZoneId, endTime, endTimeZoneId, ChronoUnit.DAYS) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Time events can not span for more than 24 hours. Consider creating a Day event instead")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private static boolean hasValidFrequencyProperties(AbstractEventRequest eventRequest, ConstraintValidatorContext context) {
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

        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY) && (eventRequest.getWeeklyRecurrenceDays() == null || eventRequest.getWeeklyRecurrenceDays().isEmpty())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide at least one day of the week for weekly repeating events")
                    .addConstraintViolation();
            return false;
        }

        if (!eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY) && eventRequest.getWeeklyRecurrenceDays() != null && !eventRequest.getWeeklyRecurrenceDays().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Weekly recurrence days are only valid for weekly repeating events")
                    .addConstraintViolation();
            return false;
        }

        // https://stackoverflow.com/questions/19825563/custom-validator-message-throwing-exception-in-implementation-of-constraintvali/19833921#19833921
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY) && eventRequest.getMonthlyRepetitionType() == null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a monthly repetition type for monthly repeating events")
                    .addConstraintViolation();
            return false;
        }

        if (!eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY) && eventRequest.getMonthlyRepetitionType() != null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Monthly repetition types are only valid for monthly repeating events")
                    .addConstraintViolation();
            return false;
        }

        // At this point getMonthlyType() will always be null
        if (eventRequest.getRepetitionDuration() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify an end date or a number of repetitions for repeating events")
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
        if (eventRequest.getRepetitionDuration().equals(RepetitionDuration.UNTIL_DATE) && eventRequest.getRepetitionEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The repetition end date is required when repetition duration is set to until a certain date")
                    .addConstraintViolation();
            return false;
        }

        // Duration is N_REPETITIONS and repetitionOccurrences is null
        if (eventRequest.getRepetitionDuration().equals(RepetitionDuration.N_REPETITIONS) && (eventRequest.getRepetitionOccurrences() == null || eventRequest.getRepetitionOccurrences() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The number of repetitions is required when repetition duration is set to a certain number of repetitions")
                    .addConstraintViolation();
            return false;
        }

        // Both end date and repetition occurrences were provided
        if (eventRequest.getRepetitionEndDate() != null && eventRequest.getRepetitionOccurrences() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify either a repetition end date or a number of repetitions. Not both")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    public static void updateCommonEventProperties(AbstractEventRequest eventRequest, AbstractEvent event) {
        event.setRepetitionFrequency(eventRequest.getRepetitionFrequency());
        event.setRepetitionStep(eventRequest.getRepetitionStep());
        event.setWeeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays());
        event.setMonthlyRepetitionType(eventRequest.getMonthlyRepetitionType());
        event.setRepetitionDuration(eventRequest.getRepetitionDuration());
        event.setRepetitionEndDate(eventRequest.getRepetitionEndDate());
        event.setRepetitionOccurrences(eventRequest.getRepetitionOccurrences());
    }

    public static void updateCommonEventSlotProperties(AbstractEventSlotRequest eventSlotRequest, AbstractEventSlot eventSlot) {
        eventSlot.setTitle(eventSlotRequest.getTitle() != null && !eventSlotRequest.getTitle().isBlank() ? eventSlotRequest.getTitle() : eventSlot.getTitle());
        eventSlot.setLocation(eventSlotRequest.getLocation() != null && !eventSlotRequest.getLocation().isBlank() ? eventSlotRequest.getLocation() : eventSlot.getLocation());
        eventSlot.setDescription(eventSlotRequest.getDescription() != null && !eventSlotRequest.getDescription().isBlank() ? eventSlotRequest.getDescription() : eventSlot.getDescription());
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

    public static Set<String> processGuestEmails(User user, Set<String> guestEmails) {
        if (guestEmails == null || guestEmails.isEmpty()) {
            return Collections.emptySet();
        }

        if (guestEmails.contains(user.getEmail())) {
            throw new ConflictException("Organizer of the event can't be added as guest");
        }

        // We filter out emails that don't contain @
        return guestEmails.stream()
                .filter(email -> email.contains("@"))
                .collect(Collectors.toSet());
    }

    public static DayEventSlotReminderRequest mapToReminderRequest(DayEventSlot eventSlot) {
        return DayEventSlotReminderRequest.builder()
                .id(eventSlot.getId())
                .title(eventSlot.getTitle())
                .startDate(eventSlot.getStartDate())
                .organizer(eventSlot.getDayEvent().getUser())
                .guestEmails(eventSlot.getGuestEmails())
                .build();
    }

    public static TimeEventSlotReminderRequest mapToReminderRequest(TimeEventSlot eventSlot) {
        return TimeEventSlotReminderRequest.builder()
                .id(eventSlot.getId())
                .title(eventSlot.getTitle())
                .startTime(eventSlot.getStartTime())
                .endTime(eventSlot.getEndTime())
                .organizer(eventSlot.getTimeEvent().getUser())
                .guestEmails(eventSlot.getGuestEmails())
                .build();
    }

    public static boolean hasSameFrequencyProperties(AbstractEventRequest eventRequest, AbstractEvent event) {
        return Objects.equals(eventRequest.getRepetitionFrequency(), event.getRepetitionFrequency())
                && Objects.equals(eventRequest.getRepetitionStep(), event.getRepetitionStep())
                && Objects.equals(eventRequest.getWeeklyRecurrenceDays(), event.getWeeklyRecurrenceDays())
                && Objects.equals(eventRequest.getMonthlyRepetitionType(), event.getMonthlyRepetitionType())
                && Objects.equals(eventRequest.getRepetitionDuration(), event.getRepetitionDuration())
                && Objects.equals(eventRequest.getRepetitionEndDate(), event.getRepetitionEndDate())
                && Objects.equals(eventRequest.getRepetitionOccurrences(), event.getRepetitionOccurrences());
    }

    public static boolean emptyEventUpdateRequestProperties(TimeEventRequest eventRequest) {
        return eventRequest.getStartTime() == null
                && eventRequest.getEndTime() == null
                && eventRequest.getStartTimeZoneId() == null
                && eventRequest.getEndTimeZoneId() == null
                && emptyEventRequestProperties(eventRequest);
    }

    public static boolean emptyEventUpdateRequestProperties(DayEventRequest eventRequest) {
        return eventRequest.getStartDate() == null
                && eventRequest.getEndDate() == null
                && emptyEventRequestProperties(eventRequest);
    }

    public static boolean emptyEventSlotUpdateRequestProperties(DayEventSlotRequest eventSlotRequest) {
        return eventSlotRequest.getStartDate() == null
                && eventSlotRequest.getEndDate() == null
                && emptyEventSlotRequestProperties(eventSlotRequest);
    }

    public static boolean emptyEventSlotUpdateRequestProperties(TimeEventSlotRequest eventSlotRequest) {
        return eventSlotRequest.getStartTime() == null
                && eventSlotRequest.getEndTime() == null
                && eventSlotRequest.getStartTimeZoneId() == null
                && eventSlotRequest.getEndTimeZoneId() == null
                && emptyEventSlotRequestProperties(eventSlotRequest);
    }

    // Checks the common fields
    private static boolean emptyEventRequestProperties(AbstractEventRequest eventRequest) {
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

    // Checks the common fields
    private static boolean emptyEventSlotRequestProperties(AbstractEventSlotRequest eventSlotRequest) {
        return ((eventSlotRequest.getTitle() == null || eventSlotRequest.getTitle().isBlank())
                && (eventSlotRequest.getLocation() == null || eventSlotRequest.getLocation().isBlank())
                && (eventSlotRequest.getDescription() == null || eventSlotRequest.getDescription().isBlank())
                && (eventSlotRequest.getGuestEmails() == null || eventSlotRequest.getGuestEmails().isEmpty()));
    }
}
