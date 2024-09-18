package org.example.google_calendar_clone.notification;

import org.example.google_calendar_clone.calendar.event.day.slot.DayEventSlotRepository;
import org.example.google_calendar_clone.email.EmailService;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.utils.EventUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final EmailService emailService;
    private final DayEventSlotRepository dayEventSlotRepository;

    void notifyDayEvents() {
        List<DayEventSlot> eventSlots = this.dayEventSlotRepository.findByStartDate(LocalDate.now().plusDays(1));

        eventSlots.stream()
                .map(EventUtils::mapToReminderRequest)
                .forEach(this.emailService::sendReminderEmail);
    }

    void notifyTimeEvents() {

    }
}
