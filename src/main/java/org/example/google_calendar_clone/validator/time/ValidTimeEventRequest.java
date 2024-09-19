package org.example.google_calendar_clone.validator.time;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeEventRequestValidator.class)
public @interface ValidTimeEventRequest {
    String message() default "Invalid TimeEventRequest configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
