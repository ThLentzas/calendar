package org.example.calendar.event.day.validator;

import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.groups.OnCreate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// https://www.baeldung.com/javax-validation-groups How to acquire a validator
// https://stackoverflow.com/questions/29069956/how-to-test-validation-annotations-of-a-class-using-junit
class DayEventCreateRequestValidatorTest {
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

        Both the DayEventCreateRequestValidator and the TimeEventCreateRequestValidator make a call to the overloaded
        method EventUtils.hasValidEventRequestProperties(). For day events, the method calls hasValidDateProperties()
        and then calls hasValidFrequencyProperties(). For time events, method calls hasValidDateTimeProperties()
        and then calls hasValidFrequencyProperties(). In this class, we test every case for the hasValidDateProperties()
        and hasValidFrequencyProperties(). We don't have to repeat the tests for the frequency properties in the
        TimeEventCreateRequestValidator
     */
    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnTrueWhenDayEventRequestIsValid() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(3)
                .recurrenceDuration(RecurrenceDuration.FOREVER)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenDayEventRequestFrequencyIsNull() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a frequency. NEVER if event does not recur");
    }

    @Test
    void shouldReturnFalseWhenStartDateIsAfterEndDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(1))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .recurrenceStep(2)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Start date must be before end date");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsWeeklyAndWeeklyRecurrenceDaysAreEmptyOrNull() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.WEEKLY)
                .recurrenceStep(1)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide at least one day of the week for weekly recurring events");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotWeeklyAndWeeklyRecurrenceDaysAreNotEmptyOrNull() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY))
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Weekly recurrence days are only valid for weekly recurring events");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsMonthlyAndRecurrenceMonthlyTypeIsNull() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(1)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a monthly recurrence type for monthly recurring events");
    }

    @Test
    void shouldReturnFalseWhenFrequencyIsNotMonthlyAndMonthlyRecurrenceTypeIsNotNull() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(1)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Monthly recurrence types are only valid for monthly recurring events");
    }

    @Test
    void shouldReturnFalseWhenRecurrenceDurationIsNullForRecurringEvents() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(1)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Specify an end date or a number of occurrences for recurring events");
    }

    @Test
    void shouldReturnFalseWhenRecurrenceDurationIsUntilDateAndRecurrenceEndDateIsNull() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(5)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The recurrence end date is required when recurrence duration is set to until a certain date");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0})
    void shouldReturnFalseWhenRecurrenceDurationIsNOccurrencesAndNumberOfOccurrencesIsNullOrZero(Integer occurrences) {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(1)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(occurrences)
                .build();
        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The number of occurrences is required when recurrence duration is set to a certain number of occurrences");
    }

    @Test
    void shouldReturnFalseWhenRecurrenceEndDateAndNumberOfOccurrencesAreBothNotNullForRecurringEvents() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .recurrenceStep(1)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.now().plusYears(1))
                .numberOfOccurrences(3)
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Specify either a recurrence end date or a number of occurrences. Not both");
    }

    @Test
    void shouldReturnFalseWhenStartDateIsNotIncludedInWeeklyRecurrenceDays() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                /*
                    Adjusts now() to the closest upcoming Thursday. If now() is already a Thursday, it remains unchanged.
                    For example, if now() is 2024-09-10 (a Tuesday), with(DayOfWeek.THURSDAY) will return 2024-09-12.
                 */
                .startDate(LocalDate.now().with(DayOfWeek.THURSDAY).plusWeeks(1))
                .endDate(LocalDate.now().with(DayOfWeek.THURSDAY).plusWeeks(1))
                .recurrenceFrequency(RecurrenceFrequency.WEEKLY)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .recurrenceStep(2)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The start date " + request.getStartDate() + " is a " + request.getStartDate().getDayOfWeek() + ", but this day is not included in the weekly recurrence days: " + request.getWeeklyRecurrenceDays());
    }


    @Test
    void shouldReturnFalseWhenRecurrenceEndDateIsBeforeTheEndDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(2)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.now().plusMonths(2))
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Recurrence end date must be after end date");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0})
    void shouldReturnFalseWhenRecurrenceStepIsNullOrZeroAndFrequencyIsNotNever(Integer step) {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event title")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .recurrenceStep(step)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.now().plusMonths(4))
                .build();

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<DayEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Specify how often you want the event to be recurring");
    }
}