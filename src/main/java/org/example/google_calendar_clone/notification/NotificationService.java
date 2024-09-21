package org.example.google_calendar_clone.notification;

import org.example.google_calendar_clone.calendar.event.slot.day.DayEventSlotRepository;
import org.example.google_calendar_clone.calendar.event.slot.time.TimeEventSlotRepository;
import org.example.google_calendar_clone.email.EmailService;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.utils.EventUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;

/*
    When it comes to testing the notification task, we need to think what we do we need to test. The methods from the
    repositories are tested and so are the email ones and all is left is our cron expression logic. In our case,
    it will run once a day at midnight for day events and every 30 minutes for time events. (00:00, 00:30, 01:00, 01:30)
    We don't need to write an actual test for the cron expressions. They are sites that test cron expressions.
    https://crontab.guru/ This is Linux based, seconds are absent, just be aware.

    There are tests that have very small intervals like every 5 seconds and use @SpyBean with @SpringBootTest to call
    verify to the method that performs the task. This will not work in our case, where we have intervals of 30 minutes
    and once a day.
    https://www.baeldung.com/spring-testing-scheduled-annotation

    Another approach is to use a ScheduledTaskHolder
    https://stackoverflow.com/questions/32319640/how-to-test-spring-scheduled
    https://stackoverflow.com/questions/64880738/test-a-scheduled-function-in-spring-boot-with-cron-property

    https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html
 */
@Service
@RequiredArgsConstructor
class NotificationService {
    private final EmailService emailService;
    private final DayEventSlotRepository dayEventSlotRepository;
    private final TimeEventSlotRepository timeEventSlotRepository;

    @Scheduled(cron = "0 0 0 * * *")
    void notifyDayEvents() {
        List<DayEventSlot> eventSlots = this.dayEventSlotRepository.findByStartDate(LocalDate.now().plusDays(1));

        eventSlots.stream()
                .map(EventUtils::mapToReminderRequest)
                .forEach(this.emailService::sendReminderEmail);
    }

    @Scheduled(cron = "0 */30 * * * *")
    void notifyTimeEvents() {
        // Starting times are stored in UTC
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(30);
        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByStartTime(dateTime);

        eventSlots.stream()
                .map(EventUtils::mapToReminderRequest)
                .forEach(this.emailService::sendReminderEmail);
    }
}
