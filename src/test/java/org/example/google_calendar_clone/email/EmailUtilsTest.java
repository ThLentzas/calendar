package org.example.google_calendar_clone.email;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationEmailRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.utils.EmailUtils;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class EmailUtilsTest {

    @Test
    void shouldFormatFrequencyDescriptionForNonRepeatingDayEvent() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-01-09"))
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();
        String expected = "Thu Jan 9, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingDailyUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-09-12"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-10-12"))
                .build();

        String expected = "Daily, from Thu Sep 12 to Sat Oct 12";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingDailyForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-12-10"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(10)
                .build();
        String expected = "Daily, 10 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNDaysUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-10-23"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(4)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-22"))
                .build();

        String expected = "Every 4 days, from Wed Oct 23 to Fri Nov 22";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNDaysForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-10-15"))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(5)
                .build();
        String expected = "Every 3 days, 5 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingWeeklyUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-05-12"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-08-11"))
                .build();
        String expected = "Weekly on Monday, Wednesday from Mon May 12, 2025 to Mon Aug 11, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingWeeklyForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-10-27"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(12)
                .build();
        String expected = "Weekly on Friday, Sunday 12 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNWeeksUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-06-14"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(3)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-09-13"))
                .build();
        String expected = "Every 3 weeks on Wednesday, Saturday from Sat Jun 14, 2025 to Sat Sep 13, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNWeeksForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-08-27"))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(15)
                .build();
        String expected = "Every 2 weeks on Wednesday, Thursday, Friday 15 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingMonthlyOnTheSameDayUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-12-09"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-02-09"))
                .build();
        String expected = "Monthly on day 9, from Mon Dec 9 to Sun Feb 9, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingMonthlyOnTheSameDayForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-01-14"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();
        String expected = "Monthly on day 14, 4 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-03-20"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-07-15"))
                .build();
        String expected = "Monthly on the third Thursday, from Thu Mar 20, 2025 to Tue Jul 15, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-06-28"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(8)
                .build();
        String expected = "Monthly on the fourth Saturday, 8 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2024-11-06"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-02-18"))
                .build();
        String expected = "Every 3 months on day 6, from Wed Nov 6 to Tue Feb 18, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-02-03"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(6)
                .build();
        String expected = "Every 2 months on day 3, 6 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-04-08"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2025-05-20"))
                .build();
        String expected = "Every 3 months on the second Tuesday, from Tue Apr 8, 2025 to Tue May 20, 2025";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-07-27"))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(5)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(14)
                .build();
        String expected = "Every 5 months on the fourth Sunday, 14 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingAnnuallyUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-04-28"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2027-04-28"))
                .build();
        String expected = "Annually on April 28, from Mon Apr 28, 2025 to Wed Apr 28, 2027";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingAnnuallyForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-07-18"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(6)
                .build();
        String expected = "Annually on July 18, 6 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNYearsUntilDate() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-09-18"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2029-09-18"))
                .build();
        String expected = "Every 2 years on September 18, from Thu Sep 18, 2025 to Tue Sep 18, 2029";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFormatFrequencyDescriptionWhenDayEventIsRepeatingEveryNYearsForNRepetitions() {
        DayEventInvitationEmailRequest emailRequest  = DayEventInvitationEmailRequest.builder()
                .startDate(LocalDate.parse("2025-11-11"))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(3)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(8)
                .build();
        String expected = "Every 3 years on November 11, 8 times";

        String actual = EmailUtils.formatFrequencyDescription(emailRequest);

        assertThat(actual).isEqualTo(expected);
    }
}
