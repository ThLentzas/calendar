package org.example.google_calendar_clone.calendar.event.slot.day.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DayEventSlotRequestValidator.class)
public @interface ValidDayEventSlotRequest {
    String message() default "Invalid DayEventSlotRequest configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
