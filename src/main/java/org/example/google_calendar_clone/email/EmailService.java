package org.example.google_calendar_clone.email;

import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotReminderRequest;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventInvitationRequest;
import org.example.google_calendar_clone.exception.ServerErrorException;
import org.example.google_calendar_clone.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void sendInvitationEmail(DayEventInvitationRequest invitationRequest) {
        String frequencyText = EmailUtils.formatFrequencyText(invitationRequest);
        String context = this.thymeleafService.setInvitationEmailContext(
                invitationRequest.getStartDate(),
                invitationRequest.getEventName(),
                invitationRequest.getOrganizer(),
                invitationRequest.getLocation(),
                invitationRequest.getDescription(),
                frequencyText);

        for (String guestEmail : invitationRequest.getGuestEmails()) {
            sendEmail(guestEmail, "Event Invitation", context);
        }
    }

    @Async
    public void sendInvitationEmail(TimeEventInvitationRequest invitationRequest) {
        String frequencyText = EmailUtils.formatFrequencyText(invitationRequest);
        String context = this.thymeleafService.setInvitationEmailContext(
                invitationRequest.getStartTime().toLocalDate(),
                invitationRequest.getEventName(),
                invitationRequest.getOrganizer(),
                invitationRequest.getLocation(),
                invitationRequest.getDescription(),
                frequencyText);

        for (String guestEmail : invitationRequest.getGuestEmails()) {
            sendEmail(guestEmail, "Event Invitation", context);
        }
    }

    @Async
    public void sendReminderEmail(DayEventSlotReminderRequest reminderRequest) {

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
