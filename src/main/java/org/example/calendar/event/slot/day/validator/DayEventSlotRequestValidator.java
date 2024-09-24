package org.example.calendar.event.slot.day.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.calendar.event.slot.day.dto.DayEventSlotRequest;
import org.example.calendar.utils.EventUtils;

public final class DayEventSlotRequestValidator implements ConstraintValidator<ValidDayEventSlotRequest, DayEventSlotRequest> {

    @Override
    public boolean isValid(DayEventSlotRequest eventSlotRequest, ConstraintValidatorContext context) {
        if (EventUtils.emptyEventSlotUpdateRequestProperties(eventSlotRequest)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        // If the user wants to update when the event should happen, they must provide both dates
        if (eventSlotRequest.getStartDate() == null && eventSlotRequest.getEndDate() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        if (eventSlotRequest.getStartDate() != null && eventSlotRequest.getEndDate() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The end date of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        return EventUtils.hasValidDateProperties(eventSlotRequest.getStartDate(), eventSlotRequest.getEndDate(), context);
    }
}