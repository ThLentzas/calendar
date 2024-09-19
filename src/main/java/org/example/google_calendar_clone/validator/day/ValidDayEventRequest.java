package org.example.google_calendar_clone.validator.day;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DayEventRequestValidator.class)
public @interface ValidDayEventRequest {
    String message() default "Invalid DayEventRequest configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
