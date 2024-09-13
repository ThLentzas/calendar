package org.example.google_calendar_clone.email;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ThymeleafService {
    private final TemplateEngine templateEngine;

    String setInvitationEmailContext(LocalDate startDate, String eventName, String organizer, String location, String frequency) {
        Context context = new Context();
        context.setVariable("month", startDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        context.setVariable("dayOfMonth", startDate.getDayOfMonth());
        context.setVariable("dayOfWeek", startDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        context.setVariable("eventName", eventName);
        context.setVariable("frequency", frequency);
        context.setVariable("organizer", organizer);
        context.setVariable("location", location);

        return this.templateEngine.process("invitation_email", context);
    }
}
