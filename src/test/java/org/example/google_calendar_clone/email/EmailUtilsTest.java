package org.example.google_calendar_clone.email;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventInvitationRequest;
import org.example.google_calendar_clone.utils.EmailUtils;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class EmailUtilsTest {

    @Test
    void shouldFormatFrequencyTextForNonRepeatingDayEvent() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-01-09"))
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();
        String expected = "Thu Jan 9, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingDailyUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-09-12"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-10-12"))
                .build();

        String expected = "Daily, from Thu Sep 12 to Sat Oct 12";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingDailyForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-12-10"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(10)
                .build();
        String expected = "Daily, 10 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingDailyForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-09-12"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Daily";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNDaysUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-10-23"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(4)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-22"))
                .build();

        String expected = "Every 4 days, from Wed Oct 23 to Fri Nov 22";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNDaysForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-10-15"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(5)
                .build();
        String expected = "Every 3 days, 5 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNDaysForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-10-23"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(8)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Every 8 days";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingWeeklyUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-05-12"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-08-11"))
                .build();
        String expected = "Weekly, on Monday, Wednesday, from Mon May 12, 2025 to Mon Aug 11, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingWeeklyForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-10-27"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(12)
                .build();
        String expected = "Weekly, on Friday, Sunday, 12 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingWeeklyForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-10-27"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Weekly, on Friday, Sunday";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNWeeksUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-06-14"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(3)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-09-13"))
                .build();
        String expected = "Every 3 weeks on Wednesday, Saturday, from Sat Jun 14, 2025 to Sat Sep 13, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNWeeksForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-08-27"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(15)
                .build();
        String expected = "Every 2 weeks on Wednesday, Thursday, Friday, 15 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNWeeksForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-08-27"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Every 2 weeks on Wednesday, Thursday, Friday";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingMonthlyOnTheSameDayUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-12-09"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-02-09"))
                .build();
        String expected = "Monthly on day 9, from Mon Dec 9 to Sun Feb 9, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingMonthlyOnTheSameDayForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-01-14"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();
        String expected = "Monthly on day 14, 4 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingMonthlyOnTheSameDayForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-01-14"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Monthly on day 14";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-03-20"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-07-15"))
                .build();
        String expected = "Monthly on the third Thursday, from Thu Mar 20, 2025 to Tue Jul 15, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-06-28"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(8)
                .build();
        String expected = "Monthly on the fourth Saturday, 8 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-06-28"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Monthly on the fourth Saturday";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2024-11-06"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-02-18"))
                .build();
        String expected = "Every 3 months on day 6, from Wed Nov 6 to Tue Feb 18, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-02-03"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(6)
                .build();
        String expected = "Every 2 months on day 3, 6 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-02-03"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Every 2 months on day 3";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-04-08"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-05-20"))
                .build();
        String expected = "Every 3 months on the second Tuesday, from Tue Apr 8, 2025 to Tue May 20, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-07-27"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(5)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(14)
                .build();
        String expected = "Every 5 months on the fourth Sunday, 14 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-07-27"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(5)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Every 5 months on the fourth Sunday";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingAnnuallyUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-04-28"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2027-04-28"))
                .build();
        String expected = "Annually on April 28, from Mon Apr 28, 2025 to Wed Apr 28, 2027";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingAnnuallyForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-07-18"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(6)
                .build();
        String expected = "Annually on July 18, 6 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingAnnuallyForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-07-18"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Annually on July 18";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNYearsUntilDate() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-09-18"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2029-09-18"))
                .build();
        String expected = "Every 2 years on September 18, from Thu Sep 18, 2025 to Tue Sep 18, 2029";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNYearsForNRepetitions() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-11-11"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(8)
                .build();
        String expected = "Every 3 years on November 11, 8 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenDayEventIsRepeatingEveryNYearsForForever() {
        DayEventInvitationRequest emailRequest  = DayEventInvitationRequest.builder()
                .startDate(LocalDate.parse("2025-11-11"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Every 3 years on November 11";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextForNonRepeatingTimeEvent() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-14T19:30"))
                .endTime(LocalDateTime.parse("2024-09-14T20:30"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi"))
                .endTimeZoneId(ZoneId.of("Africa/Nairobi"))
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();
        String expected = "Sat Sep 14, 2024 4:30pm - 5:30pm";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingDailyUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-10-31T15:00"))
                .endTime(LocalDateTime.parse("2024-10-31T15:30"))
                .startTimeZoneId(ZoneId.of("Europe/Berlin"))
                .endTimeZoneId(ZoneId.of("Europe/Berlin"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-12-04"))
                .build();

        String expected = "Daily, 2:00pm - 2:30pm, from Thu Oct 31 to Wed Dec 4";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingDailyForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-08-10T13:30"))
                .endTime(LocalDateTime.parse("2024-08-10T14:30"))
                .startTimeZoneId(ZoneId.of("Europe/Berlin"))
                .endTimeZoneId(ZoneId.of("Europe/Berlin"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(10)
                .build();

        String expected = "Daily, 11:30am - 12:30pm, 10 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingDailyForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-08-10T13:30"))
                .endTime(LocalDateTime.parse("2024-08-10T14:30"))
                .startTimeZoneId(ZoneId.of("Europe/Berlin"))
                .endTimeZoneId(ZoneId.of("Europe/Berlin"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Daily, 11:30am - 12:30pm";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNDaysUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-08-10T20:30"))
                .endTime(LocalDateTime.parse("2024-08-10T22:00"))
                .startTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .endTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-03"))
                .build();

        String expected = "Every 3 days, 5:30pm - 7:00pm, from Sat Aug 10 to Sun Nov 3";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    /*
        This time event spans in 2 different days in their respective timezones when converted to UTC it becomes
        September 16 9pm - September 17 4am which is valid 4-hour time event
     */
    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNDaysForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-16T23:00"))
                .endTime(LocalDateTime.parse("2024-09-17T08:00"))
                .startTimeZoneId(ZoneId.of("Europe/Berlin"))
                .endTimeZoneId(ZoneId.of("Asia/Dubai"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(5)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(8)
                .build();
        String expected = "Every 5 days, 9:00pm, 8 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNDaysForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-16T23:00"))
                .endTime(LocalDateTime.parse("2024-09-17T08:00"))
                .startTimeZoneId(ZoneId.of("Europe/Berlin"))
                .endTimeZoneId(ZoneId.of("Asia/Dubai"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(5)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Every 5 days, 9:00pm";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingWeeklyUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-11-17T16:30"))
                .endTime(LocalDateTime.parse("2024-11-17T18:30"))
                .startTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .endTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-01-25"))
                .build();

        String expected = "Weekly, on Friday, Saturday, Sunday, 2:30pm - 4:30pm, from Sun Nov 17 to Sat Jan 25, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingWeeklyForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-10-31T04:00"))
                .endTime(LocalDateTime.parse("2024-10-31T07:00"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("America/New_York"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();

        String expected = "Weekly, on Monday, Wednesday, 8:00am - 11:00am, 4 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingWeeklyForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-10-31T04:00"))
                .endTime(LocalDateTime.parse("2024-10-31T07:00"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("America/New_York"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Weekly, on Monday, Wednesday, 8:00am - 11:00am";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNWeeksUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-04T10:00"))
                .endTime(LocalDateTime.parse("2024-09-04T15:00"))
                .startTimeZoneId(ZoneId.of("Asia/Singapore"))
                .endTimeZoneId(ZoneId.of("Asia/Singapore"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(3)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-12-04"))
                .build();
        String expected = "Every 3 weeks on Wednesday, Saturday, 2:00am - 7:00am, from Wed Sep 4 to Wed Dec 4";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNWeeksForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-04T10:00"))
                .endTime(LocalDateTime.parse("2024-09-04T15:00"))
                .startTimeZoneId(ZoneId.of("Asia/Singapore"))
                .endTimeZoneId(ZoneId.of("Asia/Singapore"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(3)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(5)
                .build();
        String expected = "Every 3 weeks on Wednesday, Saturday, 2:00am - 7:00am, 5 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNWeeksForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-04T10:00"))
                .endTime(LocalDateTime.parse("2024-09-04T15:00"))
                .startTimeZoneId(ZoneId.of("Asia/Singapore"))
                .endTimeZoneId(ZoneId.of("Asia/Singapore"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(3)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Every 3 weeks on Wednesday, Saturday, 2:00am - 7:00am";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingMonthlyOnTheSameDayUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-10-25T10:00"))
                .endTime(LocalDateTime.parse("2024-10-25T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/Oslo"))
                .endTimeZoneId(ZoneId.of("Europe/Oslo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-06-20"))
                .build();

        String expected = "Monthly on day 25, 8:00am - 8:30am, from Fri Oct 25 to Fri Jun 20, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }


    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingMonthlyOnTheSameDayForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-05-18T10:00"))
                .endTime(LocalDateTime.parse("2024-05-18T14:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo"))
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(5)
                .build();

        String expected = "Monthly on day 18, 1:00pm - 5:00pm, 5 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingMonthlyOnTheSameDayForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-10-25T10:00"))
                .endTime(LocalDateTime.parse("2024-10-25T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/Oslo"))
                .endTimeZoneId(ZoneId.of("Europe/Oslo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Monthly on day 25, 8:00am - 8:30am";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingMonthlyOnTheSameWeekDayUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-04T09:00"))
                .endTime(LocalDateTime.parse("2024-09-04T11:00"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi"))
                .endTimeZoneId(ZoneId.of("Africa/Nairobi"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-20"))
                .build();

        String expected = "Monthly on the first Wednesday, 6:00am - 8:00am, from Wed Sep 4 to Wed Nov 20";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingMonthlyOnTheSameWeekDayForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-30T09:00"))
                .endTime(LocalDateTime.parse("2024-09-30T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();

        String expected = "Monthly on the last Monday, 8:00am - 9:30am, 4 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingMonthlyOnTheSameWeekDayForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-30T09:00"))
                .endTime(LocalDateTime.parse("2024-09-30T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Monthly on the last Monday, 8:00am - 9:30am";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNMonthsOnTheSameDayUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-10-25T10:00"))
                .endTime(LocalDateTime.parse("2024-10-25T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/Oslo"))
                .endTimeZoneId(ZoneId.of("Europe/Oslo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-06-20"))
                .build();

        String expected = "Every 2 months on day 25, 8:00am - 8:30am, from Fri Oct 25 to Fri Jun 20, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNMonthsOnTheSameDayForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-05-18T10:00"))
                .endTime(LocalDateTime.parse("2024-05-18T14:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo"))
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(4)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(5)
                .build();

        String expected = "Every 4 months on day 18, 1:00pm - 5:00pm, 5 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNMonthsOnTheSameDayForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-05-18T10:00"))
                .endTime(LocalDateTime.parse("2024-05-18T14:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo"))
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(4)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Every 4 months on day 18, 1:00pm - 5:00pm";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNMonthsOnTheSameWeekDayUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-04T09:00"))
                .endTime(LocalDateTime.parse("2024-09-04T11:00"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi"))
                .endTimeZoneId(ZoneId.of("Africa/Nairobi"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-20"))
                .build();

        String expected = "Every 3 months on the first Wednesday, 6:00am - 8:00am, from Wed Sep 4 to Wed Nov 20";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNMonthsOnTheSameWeekForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-30T09:00"))
                .endTime(LocalDateTime.parse("2024-09-30T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(6)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(5)
                .build();

        String expected = "Every 6 months on the last Monday, 8:00am - 9:30am, 5 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNMonthsOnTheSameWeekForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-09-30T09:00"))
                .endTime(LocalDateTime.parse("2024-09-30T10:30"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(6)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Every 6 months on the last Monday, 8:00am - 9:30am";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingAnnuallyUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2025-03-22T15:00"))
                .endTime(LocalDateTime.parse("2025-03-22T16:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-12-10"))
                .build();

        String expected = "Annually on March 22, 8:00pm - 9:00pm, from Sat Mar 22, 2025 to Wed Dec 10, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    /*
        The Time event starts at 18 of the month 11:00pm to 01:00am the next day. In those cases, we only show the
        starting day
     */
    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingAnnuallyForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-06-18T18:00"))
                .endTime(LocalDateTime.parse("2024-06-18T20:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2028-12-10"))
                .build();

        String expected = "Annually on June 18, 11:00pm, from Tue Jun 18 to Sun Dec 10, 2028";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingAnnuallyForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-06-18T18:00"))
                .endTime(LocalDateTime.parse("2024-06-18T20:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();

        String expected = "Annually on June 18, 11:00pm";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNYearsUntilDate() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2025-03-22T15:00"))
                .endTime(LocalDateTime.parse("2025-03-22T16:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-12-10"))
                .build();

        String expected = "Every 2 years on March 22, 8:00pm - 9:00pm, from Sat Mar 22, 2025 to Wed Dec 10, 2025";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNYearsForNRepetitions() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-06-18T18:00"))
                .endTime(LocalDateTime.parse("2024-06-18T20:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(6)
                .build();
        String expected = "Every 2 years on June 18, 11:00pm, 6 times";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyTextWhenTimeEventIsRepeatingEveryNYearsForForever() {
        TimeEventInvitationRequest emailRequest  = TimeEventInvitationRequest.builder()
                .startTime(LocalDateTime.parse("2024-06-18T18:00"))
                .endTime(LocalDateTime.parse("2024-06-18T20:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.FOREVER)
                .build();
        String expected = "Every 2 years on June 18, 11:00pm";

        String actual = EmailUtils.formatFrequencyText(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }
}
