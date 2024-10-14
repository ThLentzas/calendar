package org.example.calendar.event.time.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.event.groups.OnUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEventUpdateRequestValidatorTest {
    private Validator validator;

    /*
        The validate() method looks for any Constraints defined in the object passed as argument. In our case, it finds
        our custom validator and invokes the isValid() method. The return value of the validate() method is a
        Set<ConstraintViolation<T>> violations. Every time a constraint fails, a ConstraintViolation is created and added
        to the Set. The creation of the new ConstraintViolation object is initiated from the buildConstraintViolationWithTemplate()
        and finalized with the call to addConstraintViolation.

        If we added a name for .addConstraintViolation("frequency"); we could also asser to that

        assertThat(violation.getPropertyPath().toString()).hasToString("frequency");

        Since in the @ValidTimeEventRequest we specify a group like @ValidTimeEventRequest(groups = OnCreate.class)
        if we just validator.validate(request);  it will fail because it uses the Default.class group and is not supported
        by our annotation.

        We need to have 1 violation per test, because the set of constraints will have more than 1
        error message and, we can not be sure that iterator.next() will return the constraint we test

        Both the DayEventUpdateRequestValidator and the TimeEventUpdateRequestValidator make a call to the overloaded
        method EventUtils.hasEmptyEventUpdateRequestProperties(). For day events, the next call is it to
        EventUtils.hasRequiredDateProperties() and then to EventUtils.hasValidEventRequestProperties() which then calls
        hasValidFrequencyProperties() and hasValidDateProperties(). For time events, the next method call is to
        EventUtils.hasRequiredDateTimeProperties() and then calls EventUtils.hasValidEventRequestProperties(), which then calls
        hasValidFrequencyProperties() and hasValidDateTimeProperties(). In this class, we test the EventUtils.hasEmptyEventUpdateRequestProperties()
        and the EventUtils.hasRequiredDateTimeProperties() cases because everything else is already tested in the
        DayEventCreateRequestValidatorTest/TimeEventCreateRequestValidatorTest class.
     */
    @BeforeEach
    public void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnTrueWhenTimeEventRequestIsValid() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Dubai")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Dubai")).plusDays(1).plusMinutes(30))
                .startTimeZoneId(ZoneId.of("Asia/Dubai"))
                .endTimeZoneId(ZoneId.of("Asia/Dubai"))
                .recurrenceFrequency(RecurrenceFrequency.NEVER)
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenAllPropertiesAreInvalid() {
        TimeEventRequest request = TimeEventRequest.builder()
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("At least one field must be provided for the update");
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .endTime(LocalDateTime.now().plusMinutes(30))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = this.validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The start time of the event is required. Please provide one");
    }

    @Test
    void shouldReturnFalseWhenEndTimeIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = this.validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The end time of the event is required. Please provide one");
    }

    @Test
    void shouldReturnFalseWhenStartTimeZoneIdIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = this.validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a time zone for your start time");
    }

    @Test
    void shouldReturnFalseWhenEndTimeZoneIdIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(1))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = this.validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a time zone for your end time");
    }
}
