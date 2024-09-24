package org.example.calendar.event.day.validator;

import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.utils.EventUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class DayEventUpdateRequestValidator implements ConstraintValidator<ValidDayEventUpdateRequest, DayEventRequest> {

    @Override
    public boolean isValid(DayEventRequest eventRequest, ConstraintValidatorContext context) {
        if (EventUtils.emptyEventUpdateRequestProperties(eventRequest)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getRecurrenceFrequency() == null) {
            eventRequest.setStartDate(null);
            eventRequest.setEndDate(null);
            eventRequest.setRecurrenceStep(null);
            eventRequest.setWeeklyRecurrenceDays(null);
            eventRequest.setMonthlyRecurrenceType(null);
            eventRequest.setRecurrenceDuration(null);
            eventRequest.setRecurrenceEndDate(null);
            eventRequest.setNumberOfOccurrences(null);
            return true;
        }

        /*
            If the frequency is not null, the user must provide the start and end date.
         */
        if (eventRequest.getStartDate() == null && eventRequest.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date and the end date of the event are required")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getStartDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The end date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
