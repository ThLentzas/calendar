package org.example.google_calendar_clone.calendar.event.day.dto.validator;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    According to google calendar, if an event that has a duration of 3 days, like March 15 - March 18 can be repeated
    every day/2 days. It might not make a lot of sense, since the event is still going but google calendar
    allows it so, we follow the same logic and, we don't perform validation for those cases. Same logic applies for
    events with a duration of 3 months can be repeated while the event is still going.
 */
public class DayEventRequestValidator implements ConstraintValidator<ValidDayEventRequest, DayEventRequest> {

    @Override
    public boolean isValid(DayEventRequest value, ConstraintValidatorContext context) {
         /*
            When the user did not provide a repetition frequency we default to NEVER . An alternative would
            be to consider the request invalid.
         */
        if (value.getRepetitionFrequency() == null) {
            value.setRepetitionFrequency(RepetitionFrequency.NEVER);
        }

        /*
            Starting date is before Ending date

            When we create an event startDate and endDate will never be null since the DayEventRequest has a @NotNull.
            When we update an event since startDate and endDate are optional, they can be null, but if they are not we
            still need to make sure that the endDate is after startDate.
         */
        if (value.getStartDate() != null
                && value.getEndDate() != null
                && value.getStartDate().isAfter(value.getEndDate())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Start date must be before end date")
                    .addConstraintViolation();
            return false;
        }

        /*
            When the frequency is NEVER, we can ignore the values of the remaining fields(set them to null) and process
            the request. We also reduce the total checks.
         */
        if (value.getRepetitionFrequency().equals(RepetitionFrequency.NEVER)) {
            value.setRepetitionStep(null);
            value.setMonthlyRepetitionType(null);
            value.setRepetitionDuration(null);
            value.setRepetitionEndDate(null);
            value.setRepetitionCount(null);
            return true;
        }

        // https://stackoverflow.com/questions/19825563/custom-validator-message-throwing-exception-in-implementation-of-constraintvali/19833921#19833921
        if (value.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY)
                && value.getMonthlyRepetitionType() == null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Please provide a monthly repetition type for monthly " +
                            "repeating events")
                    .addConstraintViolation();
            return false;
        }

        if (!value.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY)
                && value.getMonthlyRepetitionType() != null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Monthly repetition types are only valid for monthly " +
                            "repeating events")
                    .addConstraintViolation();
            return false;
        }

        // At this point getMonthlyType() will always be null
        if (value.getRepetitionDuration() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Please specify an end date or a number of repetitions for" +
                            " repeating events")
                    .addConstraintViolation();
            return false;
        }

        /*
            Similar to NEVER, we don't have to check for repetition end date or repetition count is the repetition is
            FOREVER.
         */
        if (value.getRepetitionDuration().equals(RepetitionDuration.FOREVER)) {
            value.setRepetitionEndDate(null);
            value.setRepetitionCount(null);
            return true;
        }

        // Duration is UNTIL_DATE and repetitionEndDate is null
        if (value.getRepetitionDuration().equals(RepetitionDuration.UNTIL_DATE)
                && value.getRepetitionEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The repetition end date is required when repetition " +
                            "duration is set to until a certain date")
                    .addConstraintViolation();
            return false;
        }

        // Duration is N_REPETITIONS and repetitionCount is null
        if (value.getRepetitionDuration().equals(RepetitionDuration.N_REPETITIONS)
                && (value.getRepetitionCount() == null || value.getRepetitionCount() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The number of repetitions is required when repetition " +
                            "duration is set to a certain number of repetitions")
                    .addConstraintViolation();
            return false;
        }

        // Both end date and repetition count were provided
        if (value.getRepetitionEndDate() != null && value.getRepetitionCount() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify either a repetition end date or a number of " +
                            "repetitions. Not both")
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
                && value.getEndDate() != null
                && value.getRepetitionEndDate().isBefore(value.getEndDate())) {
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
