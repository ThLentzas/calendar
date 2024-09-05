package org.example.google_calendar_clone.calendar.event.day.dto.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// https://www.baeldung.com/javax-validation-groups How to acquire a validator
// https://stackoverflow.com/questions/29069956/how-to-test-validation-annotations-of-a-class-using-junit
class DayEventRequestValidatorTest {
    private Validator validator;

    /*
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
    public void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnTrueWhenDayEventRequestIsValidAndFrequencyIsNever() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.NEVER,
                null,
                null,
                null,
                null
        );
        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsMonthlyAndRepetitionMonthlyTypeIsNull() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.MONTHLY,
                null,
                null,
                null,
                null
        );
        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Please provide a monthly repetition type for monthly repeating " +
                "events");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotMonthlyAndRepetitionMonthlyTypeIsNotNull() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.DAILY,
                MonthlyRepetitionType.SAME_DAY,
                null,
                null,
                null
        );

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Monthly repetition types are only valid for monthly repeating " +
                "events");
    }

    @Test
    void shouldReturnFalseWhenRepetitionDurationIsNullForRepeatedEvents() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.MONTHLY,
                MonthlyRepetitionType.SAME_DAY,
                null,
                null,
                null
        );

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Please specify an end date or a number of repetitions for" +
                " repeating events");
    }


    @Test
    void shouldReturnTrueWhenDayEventRequestIsValidAndRepetitionDurationIsForever() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.DAILY,
                null,
                RepetitionDuration.FOREVER,
                null,
                null
        );
        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenRepetitionDurationIsUntilDateAndRepetitionEndDateIsNull() {
        DayEventRequest request = createDayEventRequest(LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.DAILY,
                null,
                RepetitionDuration.UNTIL_DATE,
                null,
                null
        );
        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The repetition end date is required when repetition duration is" +
                " set to until a certain date");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0})
    void shouldReturnFalseWhenRepetitionDurationIsNRepetitionsAndRepetitionCountIsNullOrZero(Integer repetitionCount) {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.DAILY,
                null,
                RepetitionDuration.N_REPETITIONS,
                null,
                repetitionCount
        );
        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The number of repetitions is required when repetition duration " +
                "is set to a certain number of repetitions");
    }

    @Test
    void shouldReturnFalseWhenRepetitionEndDateAndRepetitionCountAreBothNotNullForRepeatedEvents() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusDays(3),
                RepetitionFrequency.MONTHLY,
                MonthlyRepetitionType.SAME_DAY,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.now().plusYears(1),
                3
        );

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Specify either a repetition end date or a number of " +
                "repetitions. Not both");
    }

    @Test
    void shouldReturnFalseWhenStartDateIsAfterEndDate() {
        DayEventRequest request = createDayEventRequest(LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(1),
                RepetitionFrequency.MONTHLY,
                MonthlyRepetitionType.SAME_DAY,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.now().plusYears(1),
                null
        );

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Start date must be before end date");
    }

    @Test
    void shouldReturnFalseWhenRepetitionEndDateIsBeforeTheEndDate() {
        DayEventRequest request = createDayEventRequest(LocalDate.now(),
                LocalDate.now().plusMonths(3),
                RepetitionFrequency.MONTHLY,
                MonthlyRepetitionType.SAME_DAY,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.now().plusMonths(2),
                null
        );

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Repetition end date must be after end date");
    }

    private DayEventRequest createDayEventRequest(LocalDate startDate,
                                                  LocalDate endDate,
                                                  RepetitionFrequency frequency,
                                                  MonthlyRepetitionType monthlyRepetitionType,
                                                  RepetitionDuration duration,
                                                  LocalDate repetitionEndDate,
                                                  Integer repetitionCount) {
        return DayEventRequest.builder()
                .name("Event name")
                .location("Location")
                .description("Description")
                .startDate(startDate)
                .endDate(endDate)
                .guestEmails(null)  // You can omit this if null is the default value
                .repetitionFrequency(frequency)
                .repetitionStep(1)
                .monthlyRepetitionType(monthlyRepetitionType)
                .repetitionDuration(duration)
                .repetitionEndDate(repetitionEndDate)
                .repetitionCount(repetitionCount)
                .build();
    }
}