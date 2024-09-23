package org.example.google_calendar_clone.calendar.event.day.validator;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.utils.EventUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    According to google calendar, if an event that has a duration of 3 days, like March 15 - March 18 can be repeated
    every day/2 days. It might not make a lot of sense, since the event is still going but google calendar
    allows it so, we follow the same logic and, we don't perform validation for those cases. Same logic applies for
    events with a duration of 3 weeks/months/years can be repeated while the event is still going.
 */
public final class DayEventCreateRequestValidator implements ConstraintValidator<ValidDayEventCreateRequest, DayEventRequest> {

    @Override
    public boolean isValid(DayEventRequest eventRequest, ConstraintValidatorContext context) {
        if (eventRequest.getRepetitionFrequency() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a frequency. NEVER if event does not repeat")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
