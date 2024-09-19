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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

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

        We need to have 1 violation per test, because the set of constraints will have more than 1
        error message and, we can not be sure that iterator.next() will return the constraint we test

        In this validator test, we don't include the validation part of everything related to repetition. Those scenarios
        don't change between DayEventRequest and TimeEventRequest. What changes is LocalDate to LocalDateTime

        All the dates must be created dynamically relative to now(). If they are hardcoded eventually they will be in the
        past and the validation for future or present dates will consider the request invalid. We can not also call
        LocalDateTime.now() to generate those values. It will generate values with the default time zone. We need to pass
        the time zone the provided: LocalDateTime.now(ZoneId.of("Asia/Tokyo") and startTimeZoneId(ZoneId.of("Asia/Tokyo")
     */
    @BeforeEach
    public void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldReturnFalseWhenStartTimeIsInThePast() {
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
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

    // We don't have to test if end time is in the past because start time > end time if start time is in future or
    // present and end time is in the past, the condition will be true
    @Test
    void shouldReturnFalseWhenStartTimeIsAfterEndTimeWithSameTimezones() {
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
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
    void shouldReturnFalseWhenTimeEventSpansMoreThan24HoursAcrossTimeZones() {
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
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

    // Time events can't be more than 24 hours long
    @Test
    void shouldReturnFalseWhenStartTimeIsAfterEndTimeWithDifferentTimezones() {
        LocalDateTime startTime = LocalDateTime.now(ZoneId.of("America/New_York")).plusDays(2);
        LocalDateTime endTime = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).plusDays(1).plusHours(3);
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
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

    @Test
    void shouldReturnFalseWhenRepetitionEndDateIsBeforeTheEndDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .name("Event name")
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
}
