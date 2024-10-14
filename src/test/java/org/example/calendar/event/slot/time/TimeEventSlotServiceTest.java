package org.example.calendar.event.slot.time;

import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.entity.TimeEventSlot;
import org.example.calendar.event.dto.InviteGuestsRequest;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.slot.projection.AbstractEventSlotPublicProjection;
import org.example.calendar.event.slot.time.dto.TimeEventSlotRequest;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.example.calendar.event.time.TimeEventRepository;
import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.entity.TimeEvent;
import org.example.calendar.entity.User;
import org.example.calendar.exception.ConflictException;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.UserRepository;
import org.example.calendar.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.*;

/*
    The reason why the repository is not mocked, is explained in the DayEventSlotServiceTest

    Time assertions in the tests below, are in the timezone provided by the user. In the sql, scripts are in UTC, but
    we assert on the local time based on the timezone
 */
@Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
@Import({TimeEventSlotRepository.class, TimeEventRepository.class, UserRepository.class})
class TimeEventSlotServiceTest extends AbstractRepositoryTest {
    @Autowired
    private TimeEventSlotRepository timeEventSlotRepository;
    @Autowired
    private TimeEventRepository timeEventRepository;
    @Autowired
    private UserRepository userRepository;
    private TimeEventSlotService underTest;
    private static final Faker FAKER = new Faker();

    @BeforeEach
    void setup() {
        this.underTest = new TimeEventSlotService(timeEventSlotRepository, userRepository);
    }

    /*
        Calling create() of the TimeEventSlotService, will convert the startTime to UTC. When we assert on the startTime,
        we need to know offset between the provided time zone and UTC to precompute the datetime.
        In our case, during DST (from the last Sunday in March to the last Sunday in October for European Union) the
        offset is UTC + 1. It means the time in London + 1 hour from UTC. If DST is not in effect we have UTC + 0.
        date, October 11th, 2024, Europe/London will still be in Daylight Saving Time, so the offset will be UTC + 1.
        We calculate the expected dateTime as "2024-10-11T09:00"(UTC) for "2024-10-11T10:00"(Europe/London). End time
        is adjusted from the start time plus the event duration in minutes.
     */
    @Test
    void shouldCreateTimeEventSlotForNonRecurringEvent() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-10-15T10:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.NEVER)
                .build();

        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-10-15T09:00"));

        this.underTest.create(request, event);

        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(1);
        TimeEventSlotPublicProjectionAssert.assertThat(projections.get(0))
                .hasStartTime(dateTimes.get(0))
                .hasEndTime(projections.get(0).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                        event.getStartTime(),
                        event.getStartTimeZoneId(),
                        event.getEndTime(),
                        event.getEndTimeZoneId(),
                        ChronoUnit.MINUTES)))
                .hasStartTimeZoneId(event.getStartTimeZoneId())
                .hasEndTimeZoneId(event.getEndTimeZoneId())
                .hasTitle(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasEventId(event.getId());
    }

    /*
        The time "2024-03-10T02:30" for the timezone "America/New_York" is a DST gap. What is a DST gap is fully explained
        on the EventController. Java handles this case by moving the time that falls into the DST gap 1 hour forward,
        so "2024-03-10T02:30" becomes 2024-03-10T03:30-04:00, the -04:00 is the offset. At that time, DST is active so
        the offset for "America/New_York" - 4. This is why the expected date is set to "2024-03-10T07:30" (UTC), we
        consider the adjustment of the DST gap.
     */
    @Test
    void shouldCreateTimeEventSlotForNonRecurringEventDuringDSTGap() {
        // This time falls within the DST gap in New York on March 10, 2024
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-03-10T02:30"))
                .endTime(LocalDateTime.parse("2024-03-10T03:30"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("America/New_York"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.NEVER)
                .build();

        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-03-10T07:30"));
        this.underTest.create(request, event);

        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(1);
        TimeEventSlotPublicProjectionAssert.assertThat(projections.get(0))
                .hasStartTime(dateTimes.get(0))
                .hasEndTime(projections.get(0).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                        event.getStartTime(),
                        event.getStartTimeZoneId(),
                        event.getEndTime(),
                        event.getEndTimeZoneId(),
                        ChronoUnit.MINUTES)))
                .hasStartTimeZoneId(event.getStartTimeZoneId())
                .hasEndTimeZoneId(event.getEndTimeZoneId())
                .hasTitle(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasEventId(event.getId());
    }

    /*
        The time "2024-11-03T01:30" for the timezone "America/New_York" is a DST overlap. What is a DST overlap
        is fully explained on the EventController. The specific time is to occur twice, once during DST and once
        as DST ends. Java selects the 1st offset. During DST, it is -4 and when it ends is -5. In our test, we assert
        that the UTC time is "2024-11-03T05:30" which takes into consideration the 1st UTC - 4 offset
     */
    @Test
    void shouldCreateTimeEventSlotForNonRecurringEventDuringDSTOverlap() {
        // This time falls within the DST overlap in New York on November 3, 2024
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-11-03T01:30"))
                .endTime(LocalDateTime.parse("2024-11-03T02:30"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("America/New_York"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.NEVER)
                .build();

        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-11-03T05:30"));
        this.underTest.create(request, event);


        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(1);
        TimeEventSlotPublicProjectionAssert.assertThat(projections.get(0))
                .hasStartTime(dateTimes.get(0))
                .hasEndTime(projections.get(0).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(event.getStartTime(), event.getStartTimeZoneId(), event.getEndTime(), event.getEndTimeZoneId(), ChronoUnit.MINUTES)))
                .hasStartTimeZoneId(event.getStartTimeZoneId())
                .hasEndTimeZoneId(event.getEndTimeZoneId())
                .hasTitle(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasEventId(event.getId());
    }

    /*
        America/Chicago during DST is UTC - 5 otherwise UTC - 6. Our date, October 11th, 2024, America/Chicago will
        still be in Daylight Saving Time, so the offset will be UTC - 5. We calculate the expected dateTime as
        "2024-10-11T15:00"(UTC) for "2024-10-11T10:00"(America/Chicago). For the upcoming events, we use the same time
        but in different days according to the recurrence on the request
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNDaysUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-10-11T10:00"))
                .endTime(LocalDateTime.parse("2024-10-11T15:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(2)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.parse("2024-10-18"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-10-11T15:00",
                "2024-10-13T15:00",
                "2024-10-15T15:00",
                "2024-10-17T15:00"
        ));
        this.underTest.create(request, event);

        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(4);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(event.getStartTime(), event.getStartTimeZoneId(), event.getEndTime(), event.getEndTimeZoneId(), ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    /*
        This test includes the DST change and how we correctly adjust the times. In Oslo, DST ends in 27 of October.
        Every event that is to happen before th 27 should be UTC + 2, otherwise UTC + 1. We can see from the expected
        dates to be DST adjusted and since our test passes we know that we handle DST changes gracefully.
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNDaysForNOccurrences() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                // Local Oslo time before DST ends
                .startTime(LocalDateTime.parse("2024-10-25T10:00"))
                // Local Oslo time before DST ends
                .endTime(LocalDateTime.parse("2024-10-25T12:00"))
                .startTimeZoneId(ZoneId.of("Europe/Oslo"))
                .endTimeZoneId(ZoneId.of("Europe/Oslo"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(2)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(4)
                .build();
        TimeEvent event = createTimeEvent(request);

        // DST ends on 2024-10-27 in Oslo, so dates before will be in DST (UTC + 2) and after in standard time (UTC + 1)
        // Before DST ends, UTC + 2 -> UTC
        // After DST ends, UTC + 1 -> UTC
        // After DST ends, UTC + 1 -> UTC
        // After DST ends, UTC + 1 -> UTC
        // After DST ends, UTC + 1 -> UTC
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-10-25T08:00",
                "2024-10-27T09:00",
                "2024-10-29T09:00",
                "2024-10-31T09:00",
                "2024-11-02T09:00"
        ));
        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(5);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(event.getStartTime(), event.getStartTimeZoneId(), event.getEndTime(), event.getEndTimeZoneId(), ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    /*
        It is very important, the weeklyRecurrenceDays set to contain the day of the startTime. It is part of the
        validation. "2024-08-20" is a Tuesday

        "2024-08-20" is a Tuesday and we want the event to occur every 2 weeks until "2024-09-05" on Monday and
        Tuesday. After 2 weeks, the next Monday is at "2024-09-02" and Tuesday is at "2024-09-03"
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNWeeksUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-08-20T10:00"))
                .endTime(LocalDateTime.parse("2024-08-20T12:00"))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo")) // UTC + 9. No DST in Japan +9 all year round
                .endTimeZoneId(ZoneId.of("Asia/Tokyo")) // UTC + 9. No DST +9 all year round
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.WEEKLY)
                .recurrenceStep(2)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.parse("2024-09-05"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-08-20T01:00",
                "2024-09-02T01:00",
                "2024-09-03T01:00"
        ));
        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(3);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(event.getStartTime(), event.getStartTimeZoneId(), event.getEndTime(), event.getEndTimeZoneId(), ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    /*
        It is very important, the weeklyRecurrenceDays set to contain the day of the startTime. It is part of the
        validation. "2024-09-06" is a Friday

        "2024-09-06" is a Friday and we want the event to occur every 1 week, 4 times in total, on Friday and
        Saturday.
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNWeeksForNOccurrences() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-06T10:00"))
                .endTime(LocalDateTime.parse("2024-09-06T12:00"))
                .startTimeZoneId(ZoneId.of("Europe/Madrid")) // UTC + 2
                .endTimeZoneId(ZoneId.of("Europe/Madrid")) // UTC + 2
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.WEEKLY)
                .recurrenceStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(4)
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-06T08:00",
                "2024-09-07T08:00",
                "2024-09-13T08:00",
                "2024-09-14T08:00"
        ));
        this.underTest.create(request, event);

        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(4);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameDayUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-04T10:00"))
                .endTime(LocalDateTime.parse("2024-09-04T15:00"))
                .startTimeZoneId(ZoneId.of("Asia/Singapore")) // UTC + 8, no DST
                .endTimeZoneId(ZoneId.of("Asia/Singapore")) // UTC + 8, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(1)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.parse("2024-12-04"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-04T02:00",
                "2024-10-04T02:00",
                "2024-11-04T02:00",
                "2024-12-04T02:00"
        ));
        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(4);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    /*
        This case covers both DST ending and providing start and end time in different timezones. The event happens at
        31st of October, which is the last day of October and, we adjust correctly for the upcoming months 31st of January,
        January has 31 days, 28th of February, last day of a non leap year, and 30th of April that has 30 days. We adjust
        for DST that started at March 10
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameDayForNOccurrences() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-10-31T08:00"))
                .endTime(LocalDateTime.parse("2024-10-31T15:00"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("Europe/Berlin"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(2)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_DAY)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(3)
                .build();
        TimeEvent event = createTimeEvent(request);
        // UTC - 4
        // UTC - 5, DST ends
        // UTC - 5
        // UTC - 4, DST started for America/New_York at March 10
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-10-31T12:00",
                "2024-12-31T13:00",
                "2025-02-28T13:00",
                "2025-04-30T12:00"
        ));

        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        // Size is 4, the original event + 3 times that it is to occur
        assertThat(projections).hasSize(4);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(event.getStartTime(), event.getStartTimeZoneId(), event.getEndTime(), event.getEndTimeZoneId(), ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameWeekdayUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-04T09:00"))
                .endTime(LocalDateTime.parse("2024-09-04T11:00"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .endTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(1)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_WEEKDAY)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.parse("2024-11-20"))
                .build();
        TimeEvent event = createTimeEvent(request);
        // 1st Wednesday of September
        // 1st Wednesday of October
        // 1st Wednesday of November
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-04T06:00",
                "2024-10-02T06:00",
                "2024-11-06T06:00"
        ));

        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(3);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameWeekdayForNOccurrences() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-04T09:00"))
                .endTime(LocalDateTime.parse("2024-09-04T11:00"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .endTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(2)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_WEEKDAY)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(2)
                .build();
        TimeEvent event = createTimeEvent(request);
        // 1st Wednesday of September
        // 1st Wednesday of November
        // 1st Wednesday of January
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-04T06:00",
                "2024-11-06T06:00",
                "2025-01-01T06:00"
        ));
        this.underTest.create(request, event);

        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        // Size is 3, the original event + 2 times that it is to occur
        assertThat(projections).hasSize(3);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNYearsUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-05-18T10:00"))
                .endTime(LocalDateTime.parse("2024-05-18T14:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.ANNUALLY)
                .recurrenceStep(1)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.parse("2026-12-04"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-05-18T13:00", "2025-05-18T13:00", "2026-05-18T13:00"));
        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(3);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    // This test is in the last day of February for a leap year, and we see that we adjust for the next non-leap and
    // leap years.
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNYearsForNOccurrences() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-02-29T07:00"))
                .endTime(LocalDateTime.parse("2024-02-29T09:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .recurrenceFrequency(RecurrenceFrequency.ANNUALLY)
                .recurrenceStep(1)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(4)
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-02-29T10:00",
                "2025-02-28T10:00",
                "2026-02-28T10:00",
                "2027-02-28T10:00",
                "2028-02-29T10:00")
        );

        this.underTest.create(request, event);
        List<TimeEventSlotPublicProjection> projections = this.timeEventSlotRepository.findByEventAndUserId(event.getId(), 1L);

        assertThat(projections).hasSize(5);
        for (int i = 0; i < projections.size(); i++) {
            TimeEventSlotPublicProjectionAssert.assertThat(projections.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(projections.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasTitle(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasEventId(event.getId());
        }
    }

    @Test
    void shouldUpdateEventSlotsForEvent() {
        String guestEmail = FAKER.internet().emailAddress();
        TimeEventRequest eventRequest = TimeEventRequest.builder()
                .title("New title")
                .location("New location")
                .description("New description")
                .guestEmails(Set.of(guestEmail))
                .build();
        List<TimeEventSlotPublicProjection> slotPublicProjections = this.underTest.findEventSlotsByEventAndUserId(UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c"), 1L);
        List<TimeEventSlot> eventSlots = slotPublicProjections.stream()
                .map(slotProjection -> TimeEventSlot.builder()
                        .id(slotProjection.getId())
                        .title(slotProjection.getTitle())
                        .location(slotProjection.getLocation())
                        .description(slotProjection.getDescription())
                        .guestEmails(slotProjection.getGuestEmails())
                        .build())
                .collect(Collectors.toList());

        this.underTest.updateEventSlotsForEvent(eventRequest, eventSlots);
        List<TimeEventSlotPublicProjection> actual = this.underTest.findEventSlotsByEventAndUserId(UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c"), 1L);

        assertThat(actual).hasSize(3)
                .extracting(AbstractEventSlotPublicProjection::getTitle, AbstractEventSlotPublicProjection::getLocation, AbstractEventSlotPublicProjection::getDescription, AbstractEventSlotPublicProjection::getGuestEmails)
                .containsExactlyInAnyOrder(
                        tuple("New title", "New location", "New description", Set.of(guestEmail)),
                        tuple("New title", "New location", "New description", Set.of(guestEmail)),
                        tuple("New title", "New location", "New description", Set.of(guestEmail))
                );
    }

    @Test
    void shouldUpdateEventSlot() {
        UUID slotId = UUID.fromString("f8020ab5-1bc8-4b45-9d77-1a3859c264dd");
        String guestEmail = FAKER.internet().emailAddress();
        TimeEventSlotRequest eventSlotRequest = TimeEventSlotRequest.builder()
                .title("Title")
                .location("New location")
                .startTime(LocalDateTime.parse("2024-10-11T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-11T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .endTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .guestEmails(Set.of(guestEmail))
                .build();

        this.underTest.updateEventSlot(1L, slotId, eventSlotRequest);

        this.timeEventSlotRepository.findBySlotAndUserId(slotId, 1L)
                .ifPresent(projection -> {
                    assertThat(projection.getTitle()).isEqualTo("Title");
                    assertThat(projection.getLocation()).isEqualTo("New location");
                    assertThat(projection.getStarTime()).isEqualTo(LocalDateTime.parse("2024-10-11T07:00:00"));
                    assertThat(projection.getEndTime()).isEqualTo(LocalDateTime.parse("2024-10-11T12:00:00"));
                    assertThat(projection.getStartTimeZoneId()).isEqualTo(ZoneId.of("Europe/Helsinki"));
                    assertThat(projection.getEndTimeZoneId()).isEqualTo(ZoneId.of("Europe/Helsinki"));
                    assertThat(projection.getGuestEmails()).containsExactlyInAnyOrder(guestEmail);
                });
    }

    /*
        In this case, the event slot does not exist with the provided id
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForUpdateEventSlot() {
        UUID slotId = UUID.randomUUID();
        String guestEmail = FAKER.internet().emailAddress();
        TimeEventSlotRequest eventSlotRequest = TimeEventSlotRequest.builder()
                .title("Title")
                .location("New location")
                .startTime(LocalDateTime.parse("2024-10-11T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-11T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .endTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .guestEmails(Set.of(guestEmail))
                .build();

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.updateEventSlot(1L, slotId, eventSlotRequest)).withMessage("Time event slot not found with id: " + slotId);
    }

    @Test
    void shouldThrowConflictExceptionWhenOrganizerEmailIsInGuestListForUpdateEventSlot() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        TimeEventSlotRequest eventSlotRequest = TimeEventSlotRequest.builder()
                .title("Title")
                .location("New location")
                .startTime(LocalDateTime.parse("2024-10-11T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-11T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .endTimeZoneId(ZoneId.of("Europe/Helsinki"))
                .guestEmails(Set.of("joshua.wolf@hotmail.com"))
                .build();

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.updateEventSlot(1L, slotId, eventSlotRequest)).withMessage("Organizer of the event can't be added as guest");
    }

    @Test
    void shouldInviteGuests() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String guestEmail = FAKER.internet().emailAddress();
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail));

        this.underTest.inviteGuests(1L, slotId, inviteGuestsRequest);

        this.timeEventSlotRepository.findBySlotAndUserIdFetchingGuests(slotId, 1L)
                .ifPresent(projection -> assertThat(projection.getGuestEmails()).containsExactlyInAnyOrder("ericka.ankunding@hotmail.com", guestEmail));
    }

    /*
        In this case, the event slot exists the user that made the request is not the organizer
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForInviteGuests() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String guestEmail = FAKER.internet().emailAddress();
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail));

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.inviteGuests(2L, slotId, inviteGuestsRequest)).withMessage("Time event slot not found with id: " + slotId);
    }

    @Test
    void shouldThrowConflictExceptionWhenOrganizerEmailIsInGuestListForInviteGuests() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String guestEmail = FAKER.internet().emailAddress();
        // 2nd email is the email of the organizer(from the sql script)
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail, "joshua.wolf@hotmail.com"));

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.inviteGuests(1L, slotId, inviteGuestsRequest)).withMessage("Organizer of the event can't be added as guest");
    }

    // The method returns the events slots in ASC order and for the given eventId we expect 4 event slots.
    @Test
    void shouldFindEventSlotsByEventAndUserId() {
        List<TimeEventSlotPublicProjection> eventSlots = this.underTest.findEventSlotsByEventAndUserId(UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c"), 1L);

        assertThat(eventSlots).hasSize(3)
                .isSortedAccordingTo(Comparator.comparing(TimeEventSlotPublicProjection::getStartTime))
                .extracting(TimeEventSlotPublicProjection::getId)
                .containsExactly(UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43"), UUID.fromString("f8020ab5-1bc8-4b45-9d77-1a3859c264dd"), UUID.fromString("446d9d18-2a94-4bcf-b70d-b79941e9c31a"));
    }

    // User with id 3L is the organizer of the event
    @Test
    void shouldFindEventSlotByIdWhereUserIsEitherOrganizerOrInvitedGuest() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        TimeEventSlotPublicProjection expected = TimeEventSlotPublicProjection.builder()
                .id(slotId)
                .title("Event title")
                .startTime(LocalDateTime.parse("2024-10-15T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .location("Location")
                .description("Description")
                .organizer("kris.hudson")
                .guestEmails(Set.of("ericka.ankunding@hotmail.com"))
                .eventId(UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c"))
                .build();

        TimeEventSlotPublicProjection actual = this.underTest.findEventSlotById(1L, slotId);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    /*
        In this case, the event slot does not exist.
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForFindEventSlotById() {
        UUID slotId = UUID.randomUUID();

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findEventSlotById(1L, slotId)).withMessage("Time event slot not found with id: " + slotId);
    }

    @Test
    void shouldFindTimeEventSlotsInDateRangeWhereUserIsOrganizerOrInvitedAsGuest() {
        User user = this.userRepository.findAuthUserByIdOrThrow(2L);
        List<TimeEventSlotPublicProjection> projections = this.underTest.findEventSlotsByUserInDateRange(user, LocalDateTime.parse("2024-10-20T00:00:00"), ZoneId.of("UTC"), LocalDateTime.parse("2024-10-30T00:00:00"), ZoneId.of("UTC"));

        /*
            According to the sql script, the user has username = "clement.gulgowski" and email = "ericka.ankunding@hotmail.com"
            In the 1st event, they are invited as guest and in the 2nd, they are the organizer

            We could also assertThat(eventSlots).isSortedAccordingTo(Comparator.comparing(TimeEventSlotPublicProjection::getStartTime))
            In the 1st event UTC + 1(Europe/London) and in the 2nd one UTC + 9(Asia/Tokyo)
         */
        assertThat(projections).hasSize(2)
                .extracting(TimeEventSlotPublicProjection::getStartTime, TimeEventSlotPublicProjection::getGuestEmails, TimeEventSlotPublicProjection::getOrganizer)
                .containsExactly(tuple(LocalDateTime.parse("2024-10-28T22:00"), Set.of(), "clement.gulgowski"), tuple(LocalDateTime.parse("2024-10-29T09:00"), Set.of("ericka.ankunding@hotmail.com"), "kris.hudson"));
    }

    // Two delete queries will be logged, first to delete all the guest emails and then the slot itself
    @Test
    void shouldDeleteEventSlotById() {
        UUID slotId = UUID.fromString("f8020ab5-1bc8-4b45-9d77-1a3859c264dd");
        this.underTest.deleteEventSlotById(slotId, 1L);

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.deleteEventSlotById(slotId, 1L)).withMessage("Time event slot not found with id: " + slotId);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionForDeleteEventSlotById() {
        UUID slotId = UUID.randomUUID();

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.deleteEventSlotById(slotId, 2L)).withMessage("Time event slot not found with id: " + slotId);
    }

    private TimeEvent createTimeEvent(TimeEventRequest eventRequest) {
        TimeEvent timeEvent = TimeEvent.builder()
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .startTimeZoneId(eventRequest.getStartTimeZoneId())
                .endTimeZoneId(eventRequest.getEndTimeZoneId())
                .recurrenceFrequency(eventRequest.getRecurrenceFrequency())
                .recurrenceStep(eventRequest.getRecurrenceStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(eventRequest.getMonthlyRecurrenceType())
                .recurrenceDuration(eventRequest.getRecurrenceDuration())
                .recurrenceEndDate(eventRequest.getRecurrenceEndDate())
                .numberOfOccurrences(eventRequest.getNumberOfOccurrences())
                .organizerId(1L)
                .build();
        this.timeEventRepository.create(timeEvent);


        return timeEvent;
    }

    /*
        The list of dateTimes is returned in ascending order, representing the upcoming occurrences of the event.
        We retrieve the time event slots ordered by their start time. The i-th date in the list should match
        the start time of the i-th event slot if the times are computed correctly.

        For each case, the values passed to createDateTimes() represent the exact times we expect the events to occur.
        Since we convert the start time to UTC, the expected values are also in UTC adjusted based on the timezone
     */
    private List<LocalDateTime> createDateTimes(List<String> dateTimes) {
        return dateTimes.stream()
                .map(LocalDateTime::parse)
                .toList();
    }
}
