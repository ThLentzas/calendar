package org.example.google_calendar_clone.validator.day;

import org.example.google_calendar_clone.calendar.event.day.dto.UpdateDayEventRequest;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.utils.DateUtils;
import org.example.google_calendar_clone.utils.EventUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class UpdateDayEventRequestValidator
        implements ConstraintValidator<ValidUpdateDayEventRequest, UpdateDayEventRequest> {

    @Override
    public boolean isValid(UpdateDayEventRequest value, ConstraintValidatorContext context) {
        if (EventUtils.emptyUpdateRequestProperties(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        if(value.getRepetitionFrequency() == null) {
            value.setStartDate(null);
            value.setEndDate(null);
            value.setRepetitionStep(null);
            value.setWeeklyRecurrenceDays(null);
            value.setMonthlyRepetitionType(null);
            value.setRepetitionDuration(null);
            value.setRepetitionEndDate(null);
            value.setRepetitionOccurrences(null);
            return true;
        }

        /*
            If the frequency is not null, the user must provide the start and end date.
         */
        if(value.getStartDate() == null && value.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date and the end date of the event are required")
                    .addConstraintViolation();
            return false;
        }

        if(value.getStartDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        if(value.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The end date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

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
