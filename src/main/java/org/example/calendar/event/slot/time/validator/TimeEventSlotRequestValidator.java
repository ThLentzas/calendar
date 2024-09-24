package org.example.calendar.event.slot.time.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.example.calendar.event.slot.time.dto.TimeEventSlotRequest;
import org.example.calendar.utils.EventUtils;

public final class TimeEventSlotRequestValidator implements ConstraintValidator<ValidTimeEventSlotRequest, TimeEventSlotRequest> {

    @Override
    public boolean isValid(TimeEventSlotRequest eventSlotRequest, ConstraintValidatorContext context) {
        if (EventUtils.emptyEventSlotUpdateRequestProperties(eventSlotRequest)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("At least one field must be provided for the update")
                    .addConstraintViolation();
            return false;
        }

        if (eventSlotRequest.getStartTime() == null && eventSlotRequest.getEndTime() != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The start time of the event is required. Please provide one")
                    .addConstraintViolation();

            return false;
        }

        if (eventSlotRequest.getStartTime() != null && eventSlotRequest.getEndTime() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("The end time of the event is required. Please provide one")
                    .addConstraintViolation();
            return false;
        }

        // At this point either both times are null or both have values
        if (eventSlotRequest.getStartTime() != null && eventSlotRequest.getStartTimeZoneId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a time zone for your start time")
                    .addConstraintViolation();
            return false;
        }

        if (eventSlotRequest.getEndTime() != null && eventSlotRequest.getEndTimeZoneId() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a time zone for your end time")
                    .addConstraintViolation();
            return false;
        }

        if (eventSlotRequest.getStartTimeZoneId() != null && eventSlotRequest.getStartTime() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide a start time for your time zone")
                    .addConstraintViolation();
            return false;
        }

        if (eventSlotRequest.getEndTimeZoneId() != null && eventSlotRequest.getEndTime() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Provide an end time for your time zone")
                    .addConstraintViolation();
            return false;
        }

        boolean isValid = false;
        // endTime can't be null. The 1st if condition would have been true by this point.
        if (eventSlotRequest.getStartTime() != null) {
            isValid = EventUtils.hasValidDateTimeProperties(eventSlotRequest.getStartTime(), eventSlotRequest.getEndTime(), eventSlotRequest.getStartTimeZoneId(), eventSlotRequest.getEndTimeZoneId(), context);
        }
        return isValid;
    }
}
