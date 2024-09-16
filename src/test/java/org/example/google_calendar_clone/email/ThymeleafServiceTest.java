package org.example.google_calendar_clone.email;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.example.google_calendar_clone.utils.DateUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafServiceTest {
    private ThymeleafService underTest;

    /*
        https://stackoverflow.com/questions/77051481/how-to-unit-test-a-thymeleaf-template-using-spring-boot-test

        Since we don't have an autoconfigured @Bean of TemplateEngine by Spring, we have to set up one.
        Why we need this manual set up?
        Those thymeleaf related beans are autoconfigured by ThymeleafAutoConfiguration which is enabled if any
        configuration class is annotated with @EnableAutoConfiguration.
        @SpringBootApplication has this @EnableAutoConfiguration , so when you start the app in normal way ,
        ThymeleafAutoConfiguration takes effect and define the related thymeleaf beans.

        https://www.baeldung.com/spring-template-engines

        BE VERY CAREFUL WITH TRAILING SPACES
                assertThat(actual.trim()).isEqualTo(expected.trim());

        I was not able to find an extra white space in the test template which would result in the test to fail, when
        location was null, and the <p> will not be rendered. I didn't figure out what is placed instead.

        Replaces all sequences of whitespace characters (spaces, tabs, newlines) with a single space
        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
     */
    @BeforeEach
    void setup() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(new StaticApplicationContext());
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        TemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        underTest = new ThymeleafService(templateEngine);
    }

    @Test
    void shouldSetContextForNonRepeatingDayEvent() throws IOException {
        String path = "src/test/resources/templates/day/never/non_repeating.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-01-09"),
                "Event name",
                "Organizer",
                null,
                "Thu Jan 9, 2025"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingDailyUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/daily/daily_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-12"),
                "Event name",
                "Organizer",
                "Location",
                "Daily, from Thu Sep 12 to Sat Oct 12"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingDailyForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/daily/daily_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-12-10"),
                "Event name",
                "Organizer",
                null,
                "Daily, 10 times"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingDailyForForever() throws IOException {
        String path = "src/test/resources/templates/day/daily/daily_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-12-10"),
                "Event name",
                "Organizer",
                null,
                "Daily"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNDaysUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/daily/every_n_days_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-23"),
                "Event name",
                "Organizer",
                null,
                "Every 4 days, from Wed Oct 23 to Fri Nov 22"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNDaysForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/daily/every_n_days_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-15"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 days, 5 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNDaysForForever() throws IOException {
        String path = "src/test/resources/templates/day/daily/every_n_days_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-15"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 days"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatedWeeklyUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/weekly/weekly_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-05-12"),
                "Event name",
                "Organizer",
                "Location",
                "Weekly on Monday, Wednesday from Mon May 12, 2025 to Mon Aug 11, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatedWeeklyForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/weekly/weekly_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-27"),
                "Event name",
                "Organizer",
                null,
                "Weekly on Friday, Sunday 12 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatedWeeklyForForever() throws IOException {
        String path = "src/test/resources/templates/day/weekly/weekly_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-27"),
                "Event name",
                "Organizer",
                null,
                "Weekly on Friday, Sunday"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatedEveryNWeeksUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/weekly/every_n_weeks_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-06-14"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 weeks on Wednesday, Saturday from Sat Jun 14, 2025 to Sat Sep 13, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatedEveryNWeeksForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/weekly/every_n_weeks_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-08-27"),
                "Event name",
                "Organizer",
                "Location",
                "Every 2 weeks on Wednesday, Thursday, Friday 15 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatedEveryNWeeksForForever() throws IOException {
        String path = "src/test/resources/templates/day/weekly/every_n_weeks_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-08-27"),
                "Event name",
                "Organizer",
                "Location",
                "Every 2 weeks on Wednesday, Thursday"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingMonthlyOnTheSameDayUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/monthly/monthly_same_day_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-12-09"),
                "Event name",
                "Organizer",
                "Location",
                "Monthly on day 9, from Mon Dec 9 to Sun Feb 9, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingMonthlyOnTheSameDayForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/monthly/monthly_same_day_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-01-14"),
                "Event name",
                "Organizer",
                "Location",
                "Monthly on day 14, 4 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingMonthlyOnTheSameDayForForever() throws IOException {
        String path = "src/test/resources/templates/day/monthly/monthly_same_day_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-01-14"),
                "Event name",
                "Organizer",
                "Location",
                "Monthly on day 14"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/monthly/monthly_same_weekday_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-03-20"),
                "Event name",
                "Organizer",
                null,
                "Monthly on the third Thursday, from Thu Mar 20, 2025 to Tue Jul 15, 2025"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/monthly/monthly_same_weekday_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-06-28"),
                "Event name",
                "Organizer",
                null,
                "Monthly on the fourth Saturday, 8 times"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingMonthlyOnTheSameWeekDayForForever() throws IOException {
        String path = "src/test/resources/templates/day/monthly/monthly_same_weekday_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-06-28"),
                "Event name",
                "Organizer",
                null,
                "Monthly on the fourth Saturday"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/monthly/every_n_months_same_day_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-11-06"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 months on day 6, from Wed Nov 6 to Tue Feb 18, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/monthly/every_n_months_same_day_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-02-03"),
                "Event name",
                "Organizer",
                "Location",
                "Every 2 months on day 3, 6 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNMonthsOnTheSameDayForForever() throws IOException {
        String path = "src/test/resources/templates/day/monthly/every_n_months_same_day_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-02-03"),
                "Event name",
                "Organizer",
                "Location",
                "Every 2 months on day 3"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/monthly/every_n_months_same_weekday_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-04-08"),
                "Event name",
                "Organizer",
                "Location",
                "Every 2 months on the second Tuesday, from Tue Apr 8, 2025 to Tue May 20, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/monthly/every_n_months_same_weekday_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-07-27"),
                "Event name",
                "Organizer",
                "Location",
                "Every 5 months on the fourth Sunday, 14 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNMonthsOnTheSameWeekDayForForever() throws IOException {
        String path = "src/test/resources/templates/day/monthly/every_n_months_same_weekday_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-07-27"),
                "Event name",
                "Organizer",
                "Location",
                "Every 5 months on the fourth Sunday"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingAnnuallyUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/annually/annually_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-04-28"),
                "Event name",
                "Organizer",
                "Location",
                "Annually on April 28, from Mon Apr 28, 2025 to Wed Apr 28, 2027"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingAnnuallyForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/annually/annually_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-07-18"),
                "Event name",
                "Organizer",
                null,
                "Annually on July 18, 6 times"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingAnnuallyForForever() throws IOException {
        String path = "src/test/resources/templates/day/annually/annually_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-07-18"),
                "Event name",
                "Organizer",
                null,
                "Annually on July 18"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNYearsUntilDate() throws IOException {
        String path = "src/test/resources/templates/day/annually/every_n_years_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-09-18"),
                "Event name",
                "Organizer",
                "Location",
                "Every 2 years on September 18, from Thu Sep 18, 2025 to Tue Sep 18, 2029"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNYearsForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/day/annually/every_n_years_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-11-11"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 years on November 11, 8 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenDayEventIsRepeatingEveryNYearsForForever() throws IOException {
        String path = "src/test/resources/templates/day/annually/every_n_years_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2025-11-11"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 years on November 11"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-09-16T02:30", endTime: "2024-09-16T03:30", zoneId: ZoneId.of("America/Argentina/Mendoza")
    @Test
    void shouldSetContextForNonRepeatingTimeEvent() throws IOException {
        String path = "src/test/resources/templates/time/never/non_repeating.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-16"),
                "Event name",
                "Organizer",
                null,
                "Mon Sep 16 8:30am – 9:30am"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    // startTime: "2024-10-31T15:00", endTime: "2024-10-31T15:30", zoneId: ZoneId.of("Europe/Berlin"
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingDailyUntilDate() throws IOException {
        String path = "src/test/resources/templates/time/daily/daily_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-31"),
                "Event name",
                "Organizer",
                "Location",
                "Daily, 2:00pm - 2:30pm, from Thu Oct 31 to Wed Dec 4"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-08-10T13:30", endTime: "2024-08-10T14:30", zoneId: ZoneId.of("Europe/Berlin")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingDailyForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/time/daily/daily_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-08-10"),
                "Event name",
                "Organizer",
                null,
                "Daily, 11:30am - 12:30pm, 10 times"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextWhenTimeEventIsRepeatingDailyForForever() throws IOException {
        String path = "src/test/resources/templates/time/daily/daily_forever.html";
       String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-08-10"),
                "Event name",
                "Organizer",
                null,
                "Daily, 11:30am - 12:30pm"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    // startTime: "2024-08-10T20:30", endTime: "2024-08-10T22:00", zoneId: ZoneId.of("Europe/Helsinki")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingEveryNDaysUntilDate() throws IOException {
        String path = "src/test/resources/templates/time/daily/every_n_days_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-08-10"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 days, 5:30pm - 7:00pm, from Sat Aug 10 to Sun Nov 3"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-09-16T23:00", endTime: "2024-09-17T08:00", zoneId: ZoneId.of("Europe/Berlin"), ZoneId.of("Asia/Dubai")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingEveryNDaysForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/time/daily/every_n_days_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-16"),
                "Event name",
                "Organizer",
                "Location",
                "Every 5 days, 9:00pm, 8 times"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-09-16T23:00", endTime: "2024-09-17T08:00", zoneId: ZoneId.of("Europe/Berlin"), ZoneId.of("Asia/Dubai")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingEveryNDaysForForever() throws IOException {
        String path = "src/test/resources/templates/time/daily/every_n_days_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-16"),
                "Event name",
                "Organizer",
                "Location",
                "Every 5 days, 9:00pm"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-11-17T16:30", endTime: "2024-11-17T18:30", zoneId: ZoneId.of("Europe/Helsinki")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingWeeklyUntilDate() throws IOException {
        String path = "src/test/resources/templates/time/weekly/weekly_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-11-17"),
                "Event name",
                "Organizer",
                "Location",
                "Weekly, on Friday, Saturday, Sunday, 2:30pm - 4:30pm, from Sun Nov 17 to Sat Jan 25, 2025"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-10-31T04:00", endTime: "2024-10-31T07:00", zoneId: ZoneId.of("America/New_York")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingWeeklyForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/time/weekly/weekly_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-31"),
                "Event name",
                "Organizer",
                null,
                "Weekly, on Monday, Wednesday, 8:00am - 11:00am, 4 times"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    // startTime: "2024-10-31T04:00", endTime: "2024-10-31T07:00", zoneId: ZoneId.of("America/New_York")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingWeeklyForForever() throws IOException {
        String path = "src/test/resources/templates/time/weekly/weekly_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-31"),
                "Event name",
                "Organizer",
                null,
                "Weekly, on Monday, Wednesday, 8:00am - 11:00am"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    // startTime: "2024-09-04T10:00", endTime: "2024-09-04T15:00", zoneId: ZoneId.of("Asia/Singapore")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingEveryNWeeksUntilDate() throws IOException {
        String path = "src/test/resources/templates/time/weekly/every_n_weeks_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-04"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 weeks on Wednesday, Saturday, 2:00am - 7:00am, from Wed Sep 4 to Wed Dec 4"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-09-04T10:00", endTime: "2024-09-04T15:00", zoneId: ZoneId.of("Asia/Singapore")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingEveryNWeeksForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/time/weekly/every_n_weeks_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-04"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 weeks on Wednesday, Saturday, 2:00am - 7:00am, 5 times"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-09-04T10:00", endTime: "2024-09-04T15:00", zoneId: ZoneId.of("Asia/Singapore")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingEveryNWeeksForForever() throws IOException {
        String path = "src/test/resources/templates/time/weekly/every_n_weeks_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-09-04"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 weeks on Wednesday, Saturday, 2:00am - 7:00am, 5 times"
        );

        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-10-25T10:00", endTime: "2024-10-25T10:30", zoneId: ZoneId.of("Europe/Oslo")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingMonthlyOnTheSameDayUntilDate() throws IOException {
        String path = "src/test/resources/templates/time/monthly/monthly_same_day_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-10-25"),
                "Event name",
                "Organizer",
                "Location",
                "Monthly on day 25, 8:00am - 8:30am, from Fri Oct 25 to Fri Jun 20, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    // startTime: "2024-05-18T10:00", endTime: "2024-05-18T14:00", zoneId: ZoneId.of("America/Sao_Paulo")
    @Test
    void shouldSetContextWhenTimeEventIsRepeatingMonthlyOnTheSameDayForNRepetitions() throws IOException {
        String path = "src/test/resources/templates/time/monthly/monthly_same_day_n_repetitions.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-05-18"),
                "Event name",
                "Organizer",
                "Location",
                "Monthly on day 18, 1:00pm - 5:00pm, 5 times"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenTimeEventIsRepeatingMonthlyOnTheSameDayForForever() throws IOException {
        String path = "src/test/resources/templates/time/monthly/monthly_same_day_forever.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-05-18"),
                "Event name",
                "Organizer",
                null,
                "Monthly on day 18, 1:00pm - 5:00pm"
        );

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    // toDo: fix all WeekDay and review all "," in the ThymeleafServiceTest
}