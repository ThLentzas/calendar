package org.example.google_calendar_clone.calendar.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.google_calendar_clone.calendar.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.RepetitionFrequency;

public class DayEventRequestValidator implements ConstraintValidator<ValidDayEventRequest, DayEventRequest> {

    @Override
    public boolean isValid(DayEventRequest value, ConstraintValidatorContext context) {
        /*
            When the user did not provide a repetition frequency we consider the request invalid. An alternative would
            be to set the frequency to NEVER, set all the remaining fields to null and consider the request valid. We
            would have to change the DayEventRequest from record to class.
         */
        if (value.getFrequency() == null) {
            return false;
        }

        /*
            When the frequency is NEVER, we can ignore the values of the remaining fields(set them to null) and process
            the request. We also reduce the total checks.
         */
        if (value.getFrequency().equals(RepetitionFrequency.NEVER)) {
            value.setRepetitionStep(0);
            value.setMonthlyRepetitionType(null);
            value.setRepetitionDuration(null);
            value.setRepetitionEndDate(null);
            value.setRepetitionCount(0);

            return true;
        }

        // https://stackoverflow.com/questions/19825563/custom-validator-message-throwing-exception-in-implementation-of-constraintvali/19833921#19833921
        if (value.getFrequency().equals(RepetitionFrequency.MONTHLY) && value.getMonthlyRepetitionType() == null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("You must provide a monthly repetition type for monthly " +
                            "repeated events")
                    .addConstraintViolation();
            return false;
        }

        if (!value.getFrequency().equals(RepetitionFrequency.MONTHLY) && value.getMonthlyRepetitionType() != null) {
            // Disable the default violation message from the annotation
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("You can not provide monthly repetition types for " +
                            value.getFrequency() + " repeated events")
                    .addConstraintViolation();
            return false;
        }

        // At this point getMonthlyType() will always be null
        if (value.getRepetitionDuration() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("You must provide an end date for repeated events")
                    .addConstraintViolation();
            return false;
        }

        // Both end date and repetition count were provided
        if (value.getRepetitionEndDate() != null && value.getRepetitionCount() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("You must provide either an end date or a total number of " +
                            "repetitions for repeated events")
                    .addConstraintViolation();
            return false;
        }

        if (value.getStartDate().isAfter(value.getEndDate())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("You must provide a starting date that is before the ending date")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
