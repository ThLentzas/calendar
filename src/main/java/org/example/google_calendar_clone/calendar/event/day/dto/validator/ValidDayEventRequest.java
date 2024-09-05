package org.example.google_calendar_clone.calendar.event.day.dto.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DayEventRequestValidator.class)
public @interface ValidDayEventRequest {
    String message() default "Invalid CreateDayEventRequest configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
