package org.example.google_calendar_clone.validator.day;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.utils.EventUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class UpdateDayEventRequestValidator
        implements ConstraintValidator<ValidUpdateDayEventRequest, DayEventRequest> {

    @Override
    public boolean isValid(DayEventRequest eventRequest, ConstraintValidatorContext context) {
        if (EventUtils.emptyUpdateRequestProperties(eventRequest)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        if(eventRequest.getRepetitionFrequency() == null) {
            eventRequest.setStartDate(null);
            eventRequest.setEndDate(null);
            eventRequest.setRepetitionStep(null);
            eventRequest.setWeeklyRecurrenceDays(null);
            eventRequest.setMonthlyRepetitionType(null);
            eventRequest.setRepetitionDuration(null);
            eventRequest.setRepetitionEndDate(null);
            eventRequest.setRepetitionOccurrences(null);
            return true;
        }

        /*
            If the frequency is not null, the user must provide the start and end date.
         */
        if(eventRequest.getStartDate() == null && eventRequest.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date and the end date of the event are required")
                    .addConstraintViolation();
            return false;
        }

        if(eventRequest.getStartDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        if(eventRequest.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The end date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
