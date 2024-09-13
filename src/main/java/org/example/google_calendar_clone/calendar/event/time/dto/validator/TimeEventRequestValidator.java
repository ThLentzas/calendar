package org.example.google_calendar_clone.calendar.event.time.dto.validator;

import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.utils.DateUtils;
import org.example.google_calendar_clone.utils.RepetitionUtils;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/*
    According to Google Calendar time events can have a duration of more than 1 day.
    September 30, 2024, 3:30pm – October 2, 2024, 4:30pm is a valid time event
 */
public class TimeEventRequestValidator implements ConstraintValidator<ValidTimeEventRequest, TimeEventRequest> {

    @Override
    public boolean isValid(TimeEventRequest value, ConstraintValidatorContext context) {
        /*
            The cases where we check if 1 of the time/timezone is null can only happen when we update an event. When
            we create an event they are both mandatory and @NotNull handles those cases.
         */
        if (value.getStartTime() != null && value.getStartTimeZoneId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a time zone for your end time")
                    .addConstraintViolation();
            return false;
        }

        if (value.getStartTime() == null && value.getStartTimeZoneId() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a start time for your time zone")
                    .addConstraintViolation();
            return false;
        }

        if (value.getStartTime() != null && DateUtils.futureOrPresent(value.getStartTime(), value.getStartTimeZoneId())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start time must be in the future or present")
                    .addConstraintViolation();
            return false;
        }

        if (value.getEndTime() != null && value.getEndTimeZoneId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a time zone for your start time")
                    .addConstraintViolation();
            return false;
        }

        if (value.getEndTime() == null && value.getEndTimeZoneId() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide an end time for your time zone")
                    .addConstraintViolation();
            return false;
        }

        if (value.getEndTime() != null && DateUtils.futureOrPresent(value.getEndTime(), value.getEndTimeZoneId())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("End time must be in the future or present")
                    .addConstraintViolation();
            return false;
        }

        /*
            Starting time is before Ending time

            When we create an event, startTime and endTime will never be null since the TimeEventRequest has a @NotNull.
            When we update an event since startTime and endTime are optional, they can be null, but if they are not we
            still need to make sure that the endTime is after startTime.

            We support different timezones for start and end time. We have to consider the following case.
                When user provided start time and end time in different time zones, initially it could be that
                start time < end time without taking into consideration their time-zones, simply comparing the times.
                We have to convert both to UTC and make sure that start time < end time
         */
        if (value.getStartTime() != null && value.getEndTime() != null && DateUtils.isAfter(value.getStartTime(),
                value.getStartTimeZoneId(),
                value.getEndTime(),
                value.getEndTimeZoneId())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start time must be before end time")
                    .addConstraintViolation();
            return false;
        }

        /*
            When the user did not provide a repetition frequency we default to NEVER . An alternative would
            be to consider the request invalid.
         */
        if (value.getRepetitionFrequency() == null) {
            value.setRepetitionFrequency(RepetitionFrequency.NEVER);
        }

        if (!RepetitionUtils.isValid(value, context)) {
            return false;
        }

        // If the date is 2024-09-10 (a Tuesday), the weeklyRecurrenceDays set must contain TUESDAY
        if (value.getStartTime() != null
                && value.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY)
                && !value.getWeeklyRecurrenceDays().contains(value.getStartTime().getDayOfWeek())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "The start date " + value.getStartTime() + " is a " + value.getStartTime().getDayOfWeek() +
                                    ", but this day is not included in the weekly recurrence days: " +
                                    value.getWeeklyRecurrenceDays())
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
        if (value.getRepetitionEndDate() != null && value.getEndTime() != null
                && DateUtils.isBefore(value.getRepetitionEndDate(), LocalDate.from(value.getEndTime()))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Repetition end date must be after end date")
                    .addConstraintViolation();
            return false;

        }

        /*
            If we have a frequency value other than NEVER and the user did not provide a repetition step, meaning how
            often that event will be repeated with the given frequency, we default to 1. An example would be, when the
            frequency is DAILY and repetition step is null, we set it to 1. It means that the event will be repeated
            every day until the repetitionEndDate
         */
        if (value.getRepetitionStep() == null) {
            value.setRepetitionStep(1);
        }

        return true;
    }
}
