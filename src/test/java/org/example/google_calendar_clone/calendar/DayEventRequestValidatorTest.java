package org.example.google_calendar_clone.calendar;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.example.google_calendar_clone.calendar.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.RepetitionFrequency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

// https://stackoverflow.com/questions/29069956/how-to-test-validation-annotations-of-a-class-using-junit
class DayEventRequestValidatorTest {
    private Validator validator;

    // Say about hibernate validator
    @BeforeEach
    public void setUp() {
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testValidEventRequest() {
        DayEventRequest request = new DayEventRequest();
        request.setName(null);
        request.setLocation("Conference Room");
        request.setDescription("Weekly sync");
        request.setStartDate(LocalDate.of(2023, 12, 1));
        request.setEndDate(LocalDate.of(2023, 12, 1));
        request.setGuestEmails(null);
        request.setFrequency(RepetitionFrequency.DAILY);
        request.setRepetitionStep(1);
        request.setMonthlyRepetitionType(null);
        request.setRepetitionDuration(RepetitionDuration.FOREVER);
        request.setRepetitionEndDate(null);
        request.setRepetitionCount(null);

        Set<ConstraintViolation<DayEventRequest>> violations = validator.validate(request);

        // Assert that there are no violations
        Assertions.assertTrue(violations.isEmpty());
    }
}
