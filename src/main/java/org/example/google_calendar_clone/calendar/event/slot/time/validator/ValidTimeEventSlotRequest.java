package org.example.google_calendar_clone.calendar.event.slot.time.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeEventSlotRequestValidator.class)
public @interface ValidTimeEventSlotRequest {
    String message() default "Invalid TimeEventSlotRequest configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}