package org.example.calendar.notification;

import org.example.calendar.event.slot.day.DayEventSlotRepository;
import org.example.calendar.event.slot.day.projection.DayEventSlotReminderProjection;
import org.example.calendar.event.slot.time.TimeEventSlotRepository;
import org.example.calendar.email.EmailService;
import org.example.calendar.event.slot.time.projection.TimeEventSlotReminderProjection;
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
    private final DayEventSlotRepository eventSlotRepository;
    private final TimeEventSlotRepository timeEventSlotRepository;

    @Scheduled(cron = "0 0 0 * * *")
    void notifyDayEvents() {
        List<DayEventSlotReminderProjection> eventSlots = this.eventSlotRepository.findByStartDate(LocalDate.now().plusDays(1));
        eventSlots.forEach(this.emailService::sendReminderEmail);
    }

    /*
         The difference between the event's start time and the current time in UTC is the same as the difference in the
         user's local time zone.
             Scheduled event at 3:00 PM(Europe/London)
             Event is stored as 2:00 PM UTC
             At 1:30 PM UTC, our task runs and looks for events starting at 2:00 PM UTC
             The event is found, and a notification is sent exactly 30 minutes before the event starts in the user's local
             time
         Because we store event start times in UTC, querying for events starting 30 minutes from the current UTC time
         effectively finds events that are 30 minutes away in each user's local time zone. Time zone conversions were
         already handled when the events were scheduled
     */
    @Scheduled(cron = "0 */30 * * * *")
    void notifyTimeEvents() {
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).plusMinutes(30);
        List<TimeEventSlotReminderProjection> eventSlots = this.timeEventSlotRepository.findByStartTime(dateTime);
        eventSlots.forEach(this.emailService::sendReminderEmail);
    }
}
