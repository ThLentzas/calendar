package org.example.calendar.event.slot.day.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.calendar.event.slot.day.dto.DayEventSlotRequest;
import org.example.calendar.utils.EventUtils;

/*
    Update validation logic: When the user wants to update an event slot, they see the current properties of the event
    slot, modify some/all and then the client must send all the properties not only the ones that changed. It is a way
    to handle null values. If the user provide null as a value to a property we can't know if they want to set the new
    value to null(assuming null is valid for that property), or they don't want to update the property. By forcing the
    client to send all the properties that a user can update, we know that if we have null values it means the user
    wants to set the value of that property to null, otherwise the client should send the current value of the property.
    Then we keep track of 2 states(original, modified) and we perform dynamic update as long as the properties are
    different. The process in general is a pain because of null as a value. During deserialization a missing property
    will be null, but null can also be a value provided by the user for that property. There are two alternatives:
        1. Json Patch https://www.baeldung.com/spring-rest-json-patch
        2. Make all fields optional. If the value is provided as null it will to an empty optional, otherwise if the value
        is not present during deserialization it would be null(Jackson's behaviour)

        https://www.youtube.com/watch?v=CNlLWCvazcQ Everything is explained 42:00
 */
public final class DayEventSlotRequestValidator implements ConstraintValidator<ValidDayEventSlotRequest, DayEventSlotRequest> {

    @Override
    public boolean isValid(DayEventSlotRequest eventSlotRequest, ConstraintValidatorContext context) {
        if (EventUtils.hasEmptyEventSlotUpdateRequestProperties(eventSlotRequest)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasRequiredDateProperties(eventSlotRequest.getStartDate(), eventSlotRequest.getEndDate(), context)
                && EventUtils.hasValidDateProperties(eventSlotRequest.getStartDate(), eventSlotRequest.getEndDate(), context);
    }
}