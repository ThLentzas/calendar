package org.example.google_calendar_clone.calendar.event.time.dto.validator;

import org.example.google_calendar_clone.calendar.event.day.dto.validator.OnCreate;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEventRequestValidatorTest {
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

        It is very important to have 1 violation per test, because the set of constraints will have more than 1
        error message and, we can not be sure that iterator.next() will return the constraint we test

        In this validator test, we don't include the validation part of everything related to repetition. Those scenarios
        don't change between DayEventRequest and TimeEventRequest. What changes is LocalDate to LocalDateTime
     */
    @BeforeEach
    public void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsAfterEndTime() {
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(1))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Start time must be before end time");
    }

    @Test
    void shouldReturnFalseWhenRepetitionEndDateIsBeforeTheEndDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(3))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusDays(2))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Repetition end date must be after end date");
    }

}
