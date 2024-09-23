package org.example.google_calendar_clone.calendar.event.time.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.groups.OnUpdate;
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

        In this class we test every case on the TimeEventCreateRequestValidator which covers all the cases for the
        EventUtils.hasValidEventRequestProperties(eventRequest, context), including the call to the
        hasValidDateTimeProperties(). For that reason, we don't need to repeat the tests for frequency/date times in
        the TimeEventUpdateRequestValidatorTest and date times in TimeEventSlotRequestValidatorTest
     */
    @BeforeEach
    public void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
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
    void shouldReturnFalseWhenFrequencyIsNotNullAndStartEndTimeAreNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The start time and the end time of the event are required");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotNullAndStartTimeIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .endTime(LocalDateTime.now().plusMinutes(30))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The start time of the event is required. Please provide one");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotNullAndEndTimeIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now().plusMinutes(30))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The end time of the event is required. Please provide one");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotNullAndStartTimeZoneIdIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusMinutes(50))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a time zone for your start time");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotNullAndEndTimeZoneIdIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusMinutes(50))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnUpdate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a time zone for your end time");
    }
}
