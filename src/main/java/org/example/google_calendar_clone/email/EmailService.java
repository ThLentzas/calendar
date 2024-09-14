package org.example.google_calendar_clone.email;

import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationEmailRequest;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventInvitationEmailRequest;
import org.example.google_calendar_clone.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.example.google_calendar_clone.exception.ServerErrorException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final ThymeleafService thymeleafService;
    @Value("${spring.mail.username}")
    private String sender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Async
    public void sendInvitationEmail(DayEventInvitationEmailRequest emailRequest) {
        String frequencyText = EmailUtils.formatFrequencyDescription(emailRequest);
        String context = this.thymeleafService.setInvitationEmailContext(
                emailRequest.getStartDate(),
                emailRequest.getEventName(),
                emailRequest.getOrganizer(),
                emailRequest.getLocation(),
                frequencyText);

        for (String guestEmail : emailRequest.getGuestEmails()) {
            sendEmail(guestEmail, "Event Invitation", context);
        }
    }

    @Async
    public void sendInvitationEmail(TimeEventInvitationEmailRequest emailRequest) {
//        String frequencyText = EmailUtils.formatFrequencyDescription(emailRequest);
//        String context = this.thymeleafService.setInvitationEmailContext(
//                emailRequest.getStartDate(),
//                emailRequest.getEventName(),
//                emailRequest.getOrganizer(),
//                emailRequest.getLocation(),
//                frequencyText);
//
//        for (String guestEmail : emailRequest.getGuestEmails()) {
//            sendEmail(guestEmail, "Event invitation", context);
//        }
    }

    private void sendEmail(String recipient, String subject, String emailContext) {
        MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipient);
            helper.setFrom(sender);
            helper.setSubject(subject);
            helper.setText(emailContext, true);

            this.mailSender.send(mimeMessage);
        } catch (MessagingException me) {
            logger.info(me.getMessage());
            throw new ServerErrorException("Internal Server Error");
        }
    }
}
