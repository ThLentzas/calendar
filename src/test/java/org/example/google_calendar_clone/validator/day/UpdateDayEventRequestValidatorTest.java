package org.example.google_calendar_clone.validator.day;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.example.google_calendar_clone.calendar.event.day.dto.UpdateDayEventRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateDayEventRequestValidatorTest {
    private Validator validator;

    /*
        Spring validator dependency has the Hibernate validator dependency which is the most common implementation of
        the jakarta validator.
        The validate() method looks for any Constraints defined in the object passed as argument. In our case, it finds
        our custom validator and invokes the isValid() method. The return value of the validate() method is a
        Set<ConstraintViolation<T>> violations. Every time a constraint fails, a ConstraintViolation is created and added
        to the Set. The creation of the new ConstraintViolation object is initiated from the buildConstraintViolationWithTemplate()
        and finalized with the call to addConstraintViolation.

        If we added a name for .addConstraintViolation("frequency"); we could also asser to that

        assertThat(violation.getPropertyPath().toString()).hasToString("frequency");

        Since in the @ValidEventDayRequest we specify a group like @ValidDayEventRequest(groups = OnCreate.class)
        if we just validator.validate(request);  it will fail because it uses the Default.class group and is not supported
        by our annotation.

        It is very important to have 1 violation per test, because the set of constraints will have more than 1
        error message and, we can not be sure that iterator.next() will return the constraint we test
     */
    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // The remaining cases are covered by the CreateDayEventRequestValidator. Look at UpdateDayEventRequestValidator
    // for the full explanation
    @Test
    void shouldReturnFalseWhenAllPropertiesAreNullOrEmpty() {
        UpdateDayEventRequest request = UpdateDayEventRequest.builder()
                .build();

        Set<ConstraintViolation<UpdateDayEventRequest>> violations = validator.validate(request);
        ConstraintViolation<UpdateDayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("At least one field must be provided for the update");
    }
}
