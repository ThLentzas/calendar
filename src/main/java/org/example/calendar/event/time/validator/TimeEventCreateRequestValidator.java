package org.example.calendar.event.time.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.utils.EventUtils;

/*
    According to Google Calendar time events can have a duration of more than 1 day.
    September 30, 2024, 3:30pm – October 2, 2024, 4:30pm is a valid time event
 */
public final class TimeEventCreateRequestValidator implements ConstraintValidator<ValidTimeEventCreateRequest, TimeEventRequest> {

    @Override
    public boolean isValid(TimeEventRequest eventRequest, ConstraintValidatorContext context) {
        if (eventRequest.getRecurrenceFrequency() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a frequency. NEVER if it does not recur")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
