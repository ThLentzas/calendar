package org.example.google_calendar_clone.validator.time;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.utils.EventUtils;


/*
    According to Google Calendar time events can have a duration of more than 1 day.
    September 30, 2024, 3:30pm – October 2, 2024, 4:30pm is a valid time event
 */
public final class CreateTimeEventRequestValidator
        implements ConstraintValidator<ValidCreateTimeEventRequest, TimeEventRequest> {

    @Override
    public boolean isValid(TimeEventRequest eventRequest, ConstraintValidatorContext context) {
        if (eventRequest.getRepetitionFrequency() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a frequency. NEVER if it does not repeat")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
