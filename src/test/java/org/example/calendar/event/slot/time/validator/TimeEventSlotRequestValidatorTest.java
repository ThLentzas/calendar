package org.example.calendar.event.slot.time.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.calendar.event.slot.time.dto.TimeEventSlotRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEventSlotRequestValidatorTest {
    private Validator validator;

    /*
        Spring validator dependency has the Hibernate validator dependency which is the most common implementation of
        the jakarta validator.
        The validate() method looks for any Constraints defined in the object passed as argument. In our case, it finds
        our custom validator and invokes the isValid() method. The return value of the validate() method is a
        Set<ConstraintViolation<T>> violations. Every time a constraint fails, a ConstraintViolation is created and added
        to the Set. The creation of the new ConstraintViolation object is initiated from the buildConstraintViolationWithTemplate()
        and finalized with the call to addConstraintViolation.

        If we added a name for .addConstraintViolation("frequency"); we could also asser to that:
            assertThat(violation.getPropertyPath().toString()).hasToString("frequency");

        It is very important to have 1 violation per test, because the set of constraints will have more than 1
        error message and, we can not be sure that iterator.next() will return the constraint we test

        Both the DayEventSlotRequestValidator and the TimeEventSlotRequestValidator make a call to the overloaded
        method EventUtils.hasEmptyEventSlotUpdateRequestProperties(). For day event slots, the next calls are to
        EventUtils.hasRequiredDateProperties(), and EventUtils.hasValidDateProperties(). For time event slots, the next
        calls are to EventUtils.hasRequiredDateTimeProperties() and EventUtils.hasValidDateTimeProperties(). In this
        class, we test only have to test EventUtils.hasEmptyEventSlotUpdateRequestProperties() because the remaining calls
        are already tested for the TimeEventUpdateRequestValidator.
     */
    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnTrueWhenRequestIsValid() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(1))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // Every property is null/empty/0/blank etc.
    @Test
    void shouldReturnFalseWhenAllPropertiesAreInvalid() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("At least one field must be provided for the update");
    }
}