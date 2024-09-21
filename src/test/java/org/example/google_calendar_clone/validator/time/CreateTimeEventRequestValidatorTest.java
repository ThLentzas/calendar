package org.example.google_calendar_clone.validator.time;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.validator.groups.OnCreate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTimeEventRequestValidatorTest {
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

        In this class we test every case on the CreateTimeEventRequestValidator which includes all the cases for the
        EventUtils.hasValidEventRequestProperties(eventRequest, context) method. This method is also called on the
        UpdateTimeEventRequestValidator. We don't have to repeat the tests.
     */
    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnFalseWhenTimeEventRequestFrequencyIsNull() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Dubai")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Dubai")).plusDays(2))
                .startTimeZoneId(ZoneId.of("Asia/Dubai"))
                .endTimeZoneId(ZoneId.of("Asia/Dubai"))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Provide a frequency. NEVER if it does not repeat");
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsInThePast() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).minusDays(3))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .endTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Start time must be in the future or present");
    }

    @Test
    void shouldReturnFalseWhenEndTimeIsInThePast() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).minusDays(3))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .endTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("End time must be in the future or present");
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsAfterEndTimeWithSameTimezones() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(3))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .endTimeZoneId(ZoneId.of("Asia/Tokyo"))
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
    void shouldReturnFalseWhenStartTimeIsAfterEndTimeWithDifferentTimezones() {
        LocalDateTime startTime = LocalDateTime.now(ZoneId.of("America/New_York")).plusDays(2);
        LocalDateTime endTime = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1).plusHours(3);
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(startTime)
                .endTime(endTime)
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Start time must be before end time");
    }

    // Time events can't be more than 24 hours long
    @Test
    void shouldReturnFalseWhenTimeEventSpansMoreThan24HoursAcrossTimeZones() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Dubai")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Dubai")).plusDays(2))
                .startTimeZoneId(ZoneId.of("Asia/Dubai"))
                .endTimeZoneId(ZoneId.of("Asia/Dubai"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Time events can not span for more than 24 hours. Consider creating a Day event instead");
    }

    @Test
    void shouldReturnFalseWhenStartDateIsNotIncludedInWeeklyRecurrenceDays() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                /*
                    Adjusts now() to the closest upcoming Thursday. If now() is already a Thursday, it remains unchanged.
                    For example, if now() is 2024-09-10 (a Tuesday), with(DayOfWeek.THURSDAY) will return 2024-09-12.
                 */
                .startTime(LocalDateTime.now().with(DayOfWeek.THURSDAY).plusWeeks(1))
                .endTime(LocalDateTime.now().with(DayOfWeek.THURSDAY).plusWeeks(1))
                .startTimeZoneId(ZoneId.of("Asia/Dubai"))
                .endTimeZoneId(ZoneId.of("Asia/Dubai"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("The start date " + request.getStartTime() + " is a " +
                request.getStartTime().getDayOfWeek() + ", but this day is not included in the weekly recurrence days: " +
                request.getWeeklyRecurrenceDays());
    }

    @Test
    void shouldReturnFalseWhenRepetitionEndDateIsBeforeTheEndDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1).plusMinutes(30))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .endTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now(ZoneId.of("Asia/Tokyo")))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Repetition end date must be after end date");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0})
    void shouldReturnFalseWhenRepetitionStepIsNullOrZeroAndFrequencyIsNotNever(Integer step) {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event title")
                .startTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1).plusMinutes(30))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .endTimeZoneId(ZoneId.of("Asia/Tokyo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionStep(step)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.now().plusMonths(4))
                .build();

        Set<ConstraintViolation<TimeEventRequest>> violations = validator.validate(request, OnCreate.class);
        ConstraintViolation<TimeEventRequest> violation = violations.iterator().next();

        assertThat(violation.getMessage()).isEqualTo("Specify how often you want the event to be repeated");
    }
}
