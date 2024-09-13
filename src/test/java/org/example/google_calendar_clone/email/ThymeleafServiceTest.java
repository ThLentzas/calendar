package org.example.google_calendar_clone.email;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

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
    void shouldSetContextForNonRepeatingEvent() throws Exception {
        String path = "src/test/resources/templates/never/non_repeating.html";
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
    void shouldSetContextWhenEventIsRepeatingDailyUntilDate() throws Exception {
        String path = "src/test/resources/templates/daily/daily_until_date.html";
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
    void shouldSetContextWhenEventIsRepeatingDailyForNRepetitions() throws Exception {
        String path = "src/test/resources/templates/daily/daily_n_repetitions.html";
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
    void shouldSetContextWhenEventIsRepeatingEveryNDaysUntilDate() throws Exception {
        String path = "src/test/resources/templates/daily/every_n_days_until_date.html";
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
    void shouldSetContextWhenEventIsRepeatingEveryNDaysForNRepetitions() throws Exception {
        String path = "src/test/resources/templates/daily/every_n_days_n_repetitions.html";
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
    void shouldSetContextWhenEventIsRepeatingMonthlyOnTheSameDayUntilDate() throws Exception {
        String path = "src/test/resources/templates/monthly/monthly_same_day_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-12-09"),
                "Event name",
                "Organizer",
                "Location",
                "Monthly on day 9, from Mon Dec 9, 2024 to Sun Feb 9, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenEventIsRepeatingMonthlyOnTheSameDayForNRepetitions() throws Exception {
        String path = "src/test/resources/templates/monthly/monthly_same_day_n_repetitions.html";
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
    void shouldSetContextWhenEventIsRepeatingMonthlyOnTheSameWeekDayUntilDate() throws Exception {
        String path = "src/test/resources/templates/monthly/monthly_same_weekday_until_date.html";
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
    void shouldSetContextWhenEventIsRepeatingMonthlyOnTheSameWeekDayNRepetitions() throws Exception {
        String path = "src/test/resources/templates/monthly/monthly_same_weekday_n_repetitions.html";
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
    void shouldSetContextWhenEventIsRepeatingEveryNMonthsOnTheSameDayUntilDate() throws Exception {
        String path = "src/test/resources/templates/monthly/every_n_months_same_day_until_date.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(
                LocalDate.parse("2024-11-06"),
                "Event name",
                "Organizer",
                "Location",
                "Every 3 months on day 6, from Wed Nov 6, 2024 to Tue Feb 18, 2025"
        );

        // Location is not null, don't need to replace and trim
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldSetContextWhenEventIsRepeatingEveryNMonthsOnTheSameDayForNRepetitions() throws Exception {
        String path = "src/test/resources/templates/monthly/every_n_months_same_day_n_repetitions.html";
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
    void shouldSetContextWhenEventIsRepeatingEveryNMonthsOnTheSameWeekDayUntilDate() throws Exception {
        String path = "src/test/resources/templates/monthly/every_n_months_same_weekday_until_date.html";
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
    void shouldSetContextWhenEventIsRepeatingEveryNMonthsOnTheSameWeekDayForNRepetitions() throws Exception {
        String path = "src/test/resources/templates/monthly/every_n_months_same_weekday_n_repetitions.html";
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
}