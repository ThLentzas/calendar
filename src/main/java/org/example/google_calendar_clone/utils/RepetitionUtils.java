package org.example.google_calendar_clone.utils;

import org.example.google_calendar_clone.calendar.event.AbstractEventRequest;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

import jakarta.validation.ConstraintValidatorContext;

public final class RepetitionUtils {

    private RepetitionUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("RepetitionUtils is a utility class and cannot be instantiated");
    }

    public static boolean isValid(AbstractEventRequest eventRequest, ConstraintValidatorContext context) {
        /*
            When the frequency is NEVER, we can ignore the values of the remaining fields(set them to null) and process
            the request. We also reduce the total checks.
         */
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.NEVER)) {
            eventRequest.setRepetitionStep(null);
            eventRequest.setMonthlyRepetitionType(null);
            eventRequest.setRepetitionDuration(null);
            eventRequest.setRepetitionEndDate(null);
            eventRequest.setRepetitionCount(null);
            return true;
        }

        // https://stackoverflow.com/questions/19825563/custom-validator-message-throwing-exception-in-implementation-of-constraintvali/19833921#19833921
        if (eventRequest.getRepetitionFrequency().equals(RepetitionFrequency.MONTHLY)
                && eventRequest.getMonthlyRepetitionType() == null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Please provide a monthly repetition type for monthly " +
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
            context.buildConstraintViolationWithTemplate("Please specify an end date or a number of repetitions for" +
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
            eventRequest.setRepetitionCount(null);
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
                && (eventRequest.getRepetitionCount() == null || eventRequest.getRepetitionCount() == 0)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The number of repetitions is required when repetition " +
                            "duration is set to a certain number of repetitions")
                    .addConstraintViolation();
            return false;
        }

        // Both end date and repetition count were provided
        if (eventRequest.getRepetitionEndDate() != null && eventRequest.getRepetitionCount() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Specify either a repetition end date or a number of " +
                            "repetitions. Not both")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
