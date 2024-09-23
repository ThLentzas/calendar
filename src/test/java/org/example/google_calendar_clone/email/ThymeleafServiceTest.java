package org.example.google_calendar_clone.email;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

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

        I ended up removing all the tests because what we were actually doing was to test the same thing over and over
        again with just different frequency text value

        DO NOT I REPEAT DO NOT name your test templates as your templates in src/main/resources/templates. Tests will
        pass no matter the case and context values will be overwritten. To actually test the templates provide different
        names for your test templates. invitation_email.html -> test_invitation_email.html
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
    void shouldSetContextForEventInvitationEmail() throws IOException {
        String path = "src/test/resources/templates/test_invitation_email.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setInvitationEmailContext(LocalDate.parse("2025-01-09"), null, "Organizer", null, "Description", "Thu Jan 9, 2025");

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }

    @Test
    void shouldSetContextForEventReminderEmail() throws IOException {
        // Guests are shown in order alphabetically
        Set<String> guestEmails = new TreeSet<>(Set.of("example1@example.com", "example2@example.com", "example3@example.com"));
        String path = "src/test/resources/templates/test_reminder_email.html";
        String expected = new String(Files.readAllBytes(Paths.get(path)));
        String actual = this.underTest.setReminderEmailContext("Thursday Sep 19, 2024", "Event title", "Organizer", guestEmails, "api/v1/events/day-event-slots/3075c6eb-8028-4f99-8c6c-27db1bb5cc43");

        assertThat(actual.replaceAll("\\s+", " ").trim()).isEqualTo(expected.replaceAll("\\s+", " ").trim());
    }
}