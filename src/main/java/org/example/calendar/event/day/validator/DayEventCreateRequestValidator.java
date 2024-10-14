package org.example.calendar.event.day.validator;

import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.utils.EventUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
    According to Google Calendar, an event with a duration of multiple days (e.g., March 15 to March 18) can be set to
    repeat daily or every 2 days, even though the event is still ongoing during those repetitions. It might not make a
    lot of sense, but google calendar allows it so, we follow the same logic and, we don't perform validation for those
    cases. Same logic applies for events with a duration of 3 weeks/months/years can be repeated while the event is
    still going.
 */
public final class DayEventCreateRequestValidator implements ConstraintValidator<ValidDayEventCreateRequest, DayEventRequest> {

    @Override
    public boolean isValid(DayEventRequest eventRequest, ConstraintValidatorContext context) {
        return EventUtils.hasValidEventRequestProperties(eventRequest, context);
    }
}
