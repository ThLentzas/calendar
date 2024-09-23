package org.example.google_calendar_clone.calendar.event.time.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.utils.EventUtils;

public final class TimeEventUpdateRequestValidator implements ConstraintValidator<ValidTimeEventUpdateRequest, TimeEventRequest> {

    @Override
    public boolean isValid(TimeEventRequest eventRequest, ConstraintValidatorContext context) {
        if (EventUtils.emptyEventUpdateRequestProperties(eventRequest)) {
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
