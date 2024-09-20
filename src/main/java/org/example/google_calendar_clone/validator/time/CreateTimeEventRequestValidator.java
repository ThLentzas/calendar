package org.example.google_calendar_clone.validator.time;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.CreateTimeEventRequest;
import org.example.google_calendar_clone.utils.DateUtils;
import org.example.google_calendar_clone.utils.EventUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/*
    According to Google Calendar time events can have a duration of more than 1 day.
    September 30, 2024, 3:30pm – October 2, 2024, 4:30pm is a valid time event
 */
public final class CreateTimeEventRequestValidator implements ConstraintValidator<ValidCreateTimeEventRequest, CreateTimeEventRequest> {

    @Override
    public boolean isValid(CreateTimeEventRequest value, ConstraintValidatorContext context) {
        if (DateUtils.futureOrPresent(value.getStartTime(), value.getStartTimeZoneId())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start time must be in the future or present")
                    .addConstraintViolation();
            return false;
        }

        if (DateUtils.futureOrPresent(value.getEndTime(), value.getEndTimeZoneId())) {
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
        if (DateUtils.isAfter(value.getStartTime(),
                value.getStartTimeZoneId(),
                value.getEndTime(),
                value.getEndTimeZoneId())) {
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
        if (DateUtils.timeZoneAwareDifference(value.getStartTime(),
                value.getStartTimeZoneId(),
                value.getEndTime(),
                value.getEndTimeZoneId(),
                ChronoUnit.DAYS) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Time events can not span for more than 24 hours. Consider " +
                            "creating a Day event instead")
                    .addConstraintViolation();
            return false;
        }

        if (value.getRepetitionFrequency() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a frequency. NEVER if it does not repeat")
                    .addConstraintViolation();
            return false;
        }

        if (!EventUtils.hasValidFrequencyProperties(value, context)) {
            return false;
        }

        // If the date is 2024-09-10 (a Tuesday), the weeklyRecurrenceDays set must contain TUESDAY
        if (value.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY)
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
        if (value.getRepetitionEndDate() != null
                && DateUtils.isBefore(value.getRepetitionEndDate(), LocalDate.from(value.getEndTime()))) {
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
        if (!value.getRepetitionFrequency().equals(RepetitionFrequency.NEVER) &&
                (value.getRepetitionStep() == null || value.getRepetitionStep() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify how often you want the event to be repeated")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
