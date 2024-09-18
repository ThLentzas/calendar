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

    String setInvitationEmailContext(LocalDate startDate,
                                     String eventName,
                                     String organizer,
                                     String location,
                                     String description,
                                     String frequency) {
        Context context = new Context();
        context.setVariable("month", startDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        context.setVariable("dayOfMonth", startDate.getDayOfMonth());
        context.setVariable("dayOfWeek", startDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        context.setVariable("eventName", eventName);
        context.setVariable("frequency", frequency);
        context.setVariable("organizer", organizer);
        context.setVariable("location", location);
        context.setVariable("description", description);

        return this.templateEngine.process("invitation_email", context);
    }
}

/*
    In this case, for both startDate and startTime we are interested only in the date part so, we can pass
    emailRequest.getStartTime().toLocalDate() for both DayEvent and TimeEvent. Adding the time is part of the frequency
    handled by EmailUtils. Another way was to do something like this.

     public String setInvitationEmailContext(TemporalAccessor start, String eventName, String organizer, String location, String frequency) {
        Context context = new Context();
        context.setVariable("month", start.get(ChronoField.MONTH_OF_YEAR));
        context.setVariable("dayOfMonth", start.get(ChronoField.DAY_OF_MONTH));
        context.setVariable("dayOfWeek", DayOfWeek.of(start.get(ChronoField.DAY_OF_WEEK)).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        context.setVariable("eventName", eventName);
        context.setVariable("frequency", frequency);
        context.setVariable("organizer", organizer);
        context.setVariable("location", location);

        return this.templateEngine.process("invitation_email", context);
    }
 */