package org.example.google_calendar_clone.validator.day;

import org.example.google_calendar_clone.calendar.event.day.dto.CreateDayEventRequest;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.utils.DateUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.google_calendar_clone.utils.EventUtils;

/*
    According to google calendar, if an event that has a duration of 3 days, like March 15 - March 18 can be repeated
    every day/2 days. It might not make a lot of sense, since the event is still going but google calendar
    allows it so, we follow the same logic and, we don't perform validation for those cases. Same logic applies for
    events with a duration of 3 months can be repeated while the event is still going.
 */
public final class CreateDayEventRequestValidator
        implements ConstraintValidator<ValidCreateDayEventRequest, CreateDayEventRequest> {

    @Override
    public boolean isValid(CreateDayEventRequest value, ConstraintValidatorContext context) {
        if (value.getRepetitionFrequency() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a frequency. NEVER if it does not repeat")
                    .addConstraintViolation();
            return false;
        }

        /*
            Starting date is before Ending date

            When we create an event startDate and endDate will never be null since the DayEventRequest has a @NotNull.
            When we update an event since startDate and endDate are optional, they can be null, but if they are not we
            still need to make sure that the endDate is after startDate.
         */
        if (DateUtils.isAfter(value.getStartDate(), value.getEndDate())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start date must be before end date")
                    .addConstraintViolation();
            return false;
        }

        if (!EventUtils.hasValidFrequencyProperties(value, context)) {
            return false;
        }

        // If the date is 2024-09-10 (a Tuesday), the weeklyRecurrenceDays set must contain TUESDAY
        if (value.getRepetitionFrequency().equals(RepetitionFrequency.WEEKLY)
                && !value.getWeeklyRecurrenceDays().contains(value.getStartDate().getDayOfWeek())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "The start date " + value.getStartDate() + " is a " + value.getStartDate().getDayOfWeek() +
                                    ", but this day is not included in the weekly recurrence days: " +
                                    value.getWeeklyRecurrenceDays())
                    .addConstraintViolation();
            return false;
        }

        /*
            RepetitionCount can only be null at this point. Repetition end date is before the ending date. When we
            create an event endDate will never be null since the DayEventRequest has a @NotNull. When we update an event
            since endDate is optional, it can be null, but if it is not we still need to make sure that the
            repetitionEndDate is after endDate.
         */
        if (value.getRepetitionEndDate() != null
                && DateUtils.isBefore(value.getRepetitionEndDate(), value.getEndDate())) {
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
