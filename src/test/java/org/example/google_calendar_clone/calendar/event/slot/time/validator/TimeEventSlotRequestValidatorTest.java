package org.example.google_calendar_clone.calendar.event.slot.time.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.google_calendar_clone.calendar.event.slot.time.dto.TimeEventSlotRequest;

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

        If we added a name for .addConstraintViolation("frequency"); we could also asser to that

        assertThat(violation.getPropertyPath().toString()).hasToString("frequency");

        Since in the @ValidEventDayRequest we specify a group like @ValidDayEventRequest(groups = OnCreate.class)
        if we just validator.validate(request);  it will fail because it uses the Default.class group and is not supported
        by our annotation.

        It is very important to have 1 violation per test, because the set of constraints will have more than 1
        error message and, we can not be sure that iterator.next() will return the constraint we test

        In this class we test every case on the TimeEventCreateRequestValidator which covers all the cases for the
        EventUtils.hasValidEventRequestProperties(eventRequest, context), including the call to the
        hasValidDateTimeProperties(). For that reason, we don't need to repeat the tests for frequency/date times in
        the TimeEventUpdateRequestValidatorTest and date times in TimeEventSlotRequestValidatorTest
     */
    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
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

    @Test
    void shouldReturnFalseWhenStartTimeIsNullAndEndTimeIsNot() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The start time of the event is required. Please provide one");
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsNotNullAndEndTimeIs() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The end time of the event is required. Please provide one");
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsNotNullAndStartTimeZoneIdIs() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a time zone for your start time");
    }

    @Test
    void shouldReturnFalseWhenEndTimeIsNotNullAndEndTimeZoneIdIs() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(1))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a time zone for your end time");
    }

    @Test
    void shouldReturnFalseWhenStartTimeZoneIdIsNotNullAndStartTimeIs() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a start time for your time zone");
    }

    @Test
    void shouldReturnFalseWhenEndTimeZoneIdIsNotNullAndEndTimeIs() {
        TimeEventSlotRequest request = TimeEventSlotRequest.builder()
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .build();

        Set<ConstraintViolation<TimeEventSlotRequest>> violations = this.validator.validate(request);
        ConstraintViolation<TimeEventSlotRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide an end time for your time zone");
    }
}