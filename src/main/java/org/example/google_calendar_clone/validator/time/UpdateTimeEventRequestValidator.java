package org.example.google_calendar_clone.validator.time;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;

import org.example.google_calendar_clone.utils.EventUtils;

/*
    The CreateTimeEventRequest and UpdateTimeEventRequest have the same properties. Initially, i thought i could use
    group validation with OnCreate and OnUpdate and have one class the DayEventRequest and apply different group for
    each case. This approach won't work, because we have to consider an extra case, the case where all the fields are
    invalid, null/0/empty/blank values. For example, if the name of the event is null or blank at CreateTimeEventRequest
    it is considered invalid, but for UpdateTimeEventRequest is valid (user simply did not want to update the name).
    Same logic applies for the remaining properties. We need a way to handle this specific case for the update request,
    while performing the same validation for the remaining cases as in the create request. If we just copy the common
    logic from the CreateDayEventRequestValidator we will end up with a lot of duplication. The approach below is
    something that just avoids that. I am not sure of it is good or not.
 */
public final class UpdateTimeEventRequestValidator
        implements ConstraintValidator<ValidUpdateTimeEventRequest, TimeEventRequest> {

    @Override
    public boolean isValid(TimeEventRequest eventRequest, ConstraintValidatorContext context) {
        if (EventUtils.emptyUpdateRequestProperties(eventRequest)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getRepetitionFrequency() == null) {
            eventRequest.setStartTime(null);
            eventRequest.setEndTime(null);
            eventRequest.setStartTimeZoneId(null);
            eventRequest.setEndTimeZoneId(null);
            eventRequest.setRepetitionStep(null);
            eventRequest.setWeeklyRecurrenceDays(null);
            eventRequest.setMonthlyRepetitionType(null);
            eventRequest.setRepetitionDuration(null);
            eventRequest.setRepetitionEndDate(null);
            eventRequest.setRepetitionOccurrences(null);
            return true;
        }

        if (eventRequest.getStartTime() == null && eventRequest.getEndTime() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start time and the end time of the event are required")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getStartTime() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start time of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getEndTime() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The end time of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getStartTimeZoneId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a time zone for your start time")
                    .addConstraintViolation();
            return false;
        }

        if (eventRequest.getEndTimeZoneId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a time zone for your end time")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
