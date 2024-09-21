package org.example.google_calendar_clone.calendar.event.slot.time;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.TimeEventRepository;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.slot.time.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.TimeEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ConflictException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.example.google_calendar_clone.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.*;

/*
    The reason why the repository is not mocked, is explained in the DayEventSlotServiceTest

    Time assertions in the tests below, are in the timezone provided by the user. In the sql, scripts are in UTC, but
    we assert on the local time based on the timezone
 */
@Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
class TimeEventSlotServiceTest extends AbstractRepositoryTest {
    @Autowired
    private TimeEventSlotRepository timeEventSlotRepository;
    @Autowired
    private TimeEventRepository timeEventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager testEntityManager;
    private TimeEventSlotService underTest;
    private static final Faker FAKER = new Faker();

    @BeforeEach
    void setup() {
        this.underTest = new TimeEventSlotService(timeEventSlotRepository, timeEventRepository, userRepository);
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
    void shouldCreateTimeEventSlotForNonRepeatingEvent() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-10-11T10:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();

        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-10-11T09:00"));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(1);
        TimeEventSlotAssert.assertThat(eventSlots.get(0))
                .hasStartTime(dateTimes.get(0))
                .hasEndTime(eventSlots.get(0).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                        event.getStartTime(),
                        event.getStartTimeZoneId(),
                        event.getEndTime(),
                        event.getEndTimeZoneId(),
                        ChronoUnit.MINUTES)))
                .hasStartTimeZoneId(event.getStartTimeZoneId())
                .hasEndTimeZoneId(event.getEndTimeZoneId())
                .hasName(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasTimeEvent(event);
    }

    /*
        The time "2024-03-10T02:30" for the timezone "America/New_York" is a DST gap. What is a DST gap is fully explained
        on the EventController. Java handles this case by moving the time that falls into the DST gap 1 hour forward,
        so "2024-03-10T02:30" becomes 2024-03-10T03:30-04:00, the -04:00 is the offset. At that time, DST is active so
        the offset for "America/New_York" - 4. This is why the expected date is set to "2024-03-10T07:30" (UTC), we
        consider the adjustment of the DST gap.
     */
    @Test
    void shouldCreateTimeEventSlotForNonRepeatingEventDuringDSTGap() {
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
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();

        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-03-10T07:30"));
        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(1);
        TimeEventSlotAssert.assertThat(eventSlots.get(0))
                .hasStartTime(dateTimes.get(0))
                .hasEndTime(eventSlots.get(0).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                        event.getStartTime(),
                        event.getStartTimeZoneId(),
                        event.getEndTime(),
                        event.getEndTimeZoneId(),
                        ChronoUnit.MINUTES)))
                .hasStartTimeZoneId(event.getStartTimeZoneId())
                .hasEndTimeZoneId(event.getEndTimeZoneId())
                .hasName(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasTimeEvent(event);
    }

    /*
        The time "2024-11-03T01:30" for the timezone "America/New_York" is a DST overlap. What is a DST overlap
        is fully explained on the EventController. The specific time is to be repeated twice, once during DST and once
        as DST ends. Java selects the 1st offset. During DST, it is -4 and when it ends is -5. In our test, we assert
        that the UTC time is "2024-11-03T05:30" which takes into consideration the 1st UTC - 4 offset
     */
    @Test
    void shouldCreateTimeEventSlotForNonRepeatingEventDuringDSTOverlap() {
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
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();

        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of("2024-11-03T05:30"));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(1);
        TimeEventSlotAssert.assertThat(eventSlots.get(0))
                .hasStartTime(dateTimes.get(0))
                .hasEndTime(eventSlots.get(0).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                        event.getStartTime(),
                        event.getStartTimeZoneId(),
                        event.getEndTime(),
                        event.getEndTimeZoneId(),
                        ChronoUnit.MINUTES)))
                .hasStartTimeZoneId(event.getStartTimeZoneId())
                .hasEndTimeZoneId(event.getEndTimeZoneId())
                .hasName(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasTimeEvent(event);
    }

    /*
        America/Chicago during DST is UTC - 5 otherwise UTC - 6. Our date, October 11th, 2024, America/Chicago will
        still be in Daylight Saving Time, so the offset will be UTC - 5. We calculate the expected dateTime as
        "2024-10-11T15:00"(UTC) for "2024-10-11T10:00"(America/Chicago). For the upcoming events, we use the same time
        but in different days according to the repetition on the request
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNDaysUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-10-11T10:00"))
                .endTime(LocalDateTime.parse("2024-10-11T15:00"))
                .startTimeZoneId(ZoneId.of("America/Chicago"))
                .endTimeZoneId(ZoneId.of("America/Chicago"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-10-18"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-10-11T15:00",
                "2024-10-13T15:00",
                "2024-10-15T15:00",
                "2024-10-17T15:00")
        );

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(4);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    /*
        This test includes the DST change and how we correctly adjust the times. In Oslo, DST ends in 27 of October.
        Every event that is to happen before th 27 should be UTC + 2, otherwise UTC + 1. We can see from the expected
        dates to be DST adjusted and since our test passes we know that we handle DST changes gracefully.
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNDaysForNRepetitions() {
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
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();
        TimeEvent event = createTimeEvent(request);

        // DST ends on 2024-10-27 in Oslo, so dates before will be in DST (UTC + 2) and after in standard time (UTC + 1)
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-10-25T08:00", // Before DST ends, UTC + 2 -> UTC
                "2024-10-27T09:00", // After DST ends, UTC + 1 -> UTC
                "2024-10-29T09:00", // After DST ends, UTC + 1 -> UTC
                "2024-10-31T09:00", // After DST ends, UTC + 1 -> UTC
                "2024-11-02T09:00"  // After DST ends, UTC + 1 -> UTC
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(5);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    /*
        It is very important, the weeklyRecurrenceDays set to contain the day of the startTime. It is part of the
        validation. "2024-08-20" is a Tuesday

        "2024-08-20" is a Tuesday and we want the event to be repeated every 2 weeks until "2024-09-05" on Monday and
        Tuesday. After 2 weeks, the next Monday is at "2024-09-02" and Tuesday is at "2024-09-03"
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNWeeksUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-08-20T10:00"))
                .endTime(LocalDateTime.parse("2024-08-20T12:00"))
                .startTimeZoneId(ZoneId.of("Asia/Tokyo")) // UTC + 9. No DST in Japan +9 all year round
                .endTimeZoneId(ZoneId.of("Asia/Tokyo")) // UTC + 9. No DST +9 all year round
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-09-05"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-08-20T01:00",
                "2024-09-02T01:00",
                "2024-09-03T01:00"
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(3);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    /*
        It is very important, the weeklyRecurrenceDays set to contain the day of the startTime. It is part of the
        validation. "2024-09-06" is a Friday

        "2024-09-06" is a Friday and we want the event to be repeated every 1 week, 4 times in total, on Friday and
        Saturday.
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNWeeksForNRepetitions() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-06T10:00"))
                .endTime(LocalDateTime.parse("2024-09-06T12:00"))
                .startTimeZoneId(ZoneId.of("Europe/Madrid")) // UTC + 2
                .endTimeZoneId(ZoneId.of("Europe/Madrid")) // UTC + 2
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-06T08:00",
                "2024-09-07T08:00",
                "2024-09-13T08:00",
                "2024-09-14T08:00"
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(4);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-04T10:00"))
                .endTime(LocalDateTime.parse("2024-09-04T15:00"))
                .startTimeZoneId(ZoneId.of("Asia/Singapore")) // UTC + 8, no DST
                .endTimeZoneId(ZoneId.of("Asia/Singapore")) // UTC + 8, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-12-04"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-04T02:00",
                "2024-10-04T02:00",
                "2024-11-04T02:00",
                "2024-12-04T02:00"
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(4);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    /*
        This case covers both DST ending and providing start and end time in different timezones. The event happens at
        31st of October, which is the last day of October and, we adjust correctly for the upcoming months 31st of January,
        January has 31 days, 28th of February, last day of a non leap year, and 30th of April that has 30 days. We adjust
        for DST that started at March 10
     */
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayForNRepetitions() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-10-31T08:00"))
                .endTime(LocalDateTime.parse("2024-10-31T15:00"))
                .startTimeZoneId(ZoneId.of("America/New_York"))
                .endTimeZoneId(ZoneId.of("Europe/Berlin"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(3)
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-10-31T12:00", // UTC - 4
                "2024-12-31T13:00", // UTC - 5, DST ends
                "2025-02-28T13:00", // UTC - 5
                "2025-04-30T12:00"  // UTC - 4, DST started for America/New_York at March 10
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        // Size is 4, the original event + 3 times that it is to be repeated
        assertThat(eventSlots).hasSize(4);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameWeekdayUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-04T09:00"))
                .endTime(LocalDateTime.parse("2024-09-04T11:00"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .endTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-20"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-04T06:00", // 1st Wednesday of September
                "2024-10-02T06:00", // 1st Wednesday of October
                "2024-11-06T06:00"  // 1st Wednesday of November
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(3);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameWeekdayForNRepetitions() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-09-04T09:00"))
                .endTime(LocalDateTime.parse("2024-09-04T11:00"))
                .startTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .endTimeZoneId(ZoneId.of("Africa/Nairobi")) // UTC + 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(2)
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-09-04T06:00", // 1st Wednesday of September
                "2024-11-06T06:00", // 1st Wednesday of November
                "2025-01-01T06:00"  // 1st Wednesday of January
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        // Size is 3, the original event + 2 times that it is to be repeated
        assertThat(eventSlots).hasSize(3);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNYearsUntilDate() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-05-18T10:00"))
                .endTime(LocalDateTime.parse("2024-05-18T14:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2026-12-04"))
                .build();
        TimeEvent event = createTimeEvent(request);
        List<LocalDateTime> dateTimes = createDateTimes(List.of(
                "2024-05-18T13:00",
                "2025-05-18T13:00",
                "2026-05-18T13:00"
        ));

        this.underTest.create(request, event);
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(3);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    // This test is in the last day of February for a leap year and we see that we adjust for the next non-leap and
    // leap years.
    @Test
    void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNYearsForNRepetitions() {
        TimeEventRequest request = TimeEventRequest.builder()
                .title("Event name")
                .startTime(LocalDateTime.parse("2024-02-29T07:00"))
                .endTime(LocalDateTime.parse("2024-02-29T09:00"))
                .startTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .endTimeZoneId(ZoneId.of("America/Sao_Paulo")) // UTC - 3, no DST
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
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
        this.testEntityManager.flush();

        List<TimeEventSlot> eventSlots = this.timeEventSlotRepository.findByEventId(event.getId());

        assertThat(eventSlots).hasSize(5);
        for (int i = 0; i < eventSlots.size(); i++) {
            TimeEventSlotAssert.assertThat(eventSlots.get(i))
                    .hasStartTime(dateTimes.get(i))
                    .hasEndTime(eventSlots.get(i).getStartTime().plusMinutes(DateUtils.timeZoneAwareDifference(
                            event.getStartTime(),
                            event.getStartTimeZoneId(),
                            event.getEndTime(),
                            event.getEndTimeZoneId(),
                            ChronoUnit.MINUTES)))
                    .hasStartTimeZoneId(event.getStartTimeZoneId())
                    .hasEndTimeZoneId(event.getEndTimeZoneId())
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasTimeEvent(event);
        }
    }

    @Test
    void shouldInviteGuests() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String guestEmail = FAKER.internet().emailAddress();
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail));

        this.underTest.inviteGuests(1L, slotId, inviteGuestsRequest);
        // flush() the changes to the db so the findById() does not fetch from the cache(1st level)
        this.testEntityManager.flush();

        TimeEventSlot actual = this.timeEventSlotRepository.findByIdOrThrow(slotId);
        TimeEventSlotAssert.assertThat(actual)
                .hasGuests(Set.of("ericka.ankunding@hotmail.com", guestEmail));
    }

    /*
        In this case, the event slot exists the user that made the request is not the organizer
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForInviteGuests() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String guestEmail = FAKER.internet().emailAddress();
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail));

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.inviteGuests(
                        2L, slotId, inviteGuestsRequest))
                .withMessage("Time event slot not found with id: " + slotId);
    }

    @Test
    void shouldThrowConflictExceptionWhenOrganizerEmailIsInGuestList() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        String guestEmail = FAKER.internet().emailAddress();
        // 2nd email is the email of the organizer(from the sql script)
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail, "joshua.wolf@hotmail.com"));

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.inviteGuests(
                        1L, slotId, inviteGuestsRequest))
                .withMessage("Organizer of the event can't be added as guest");
    }

    // The method returns the events slots in ASC order and for the given eventId we expect 4 event slots.
    @Test
    void shouldFindEventSlotsByEventId() {
        List<TimeEventSlotDTO> eventSlots = this.underTest.findEventSlotsByEventId(
                UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c"));

        assertThat(eventSlots).hasSize(4)
                .isSortedAccordingTo(Comparator.comparing(TimeEventSlotDTO::getStartTime))
                .extracting(TimeEventSlotDTO::getId)
                .containsExactly(
                        UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43"),
                        UUID.fromString("f8020ab5-1bc8-4b45-9d77-1a3859c264dd"),
                        UUID.fromString("446d9d18-2a94-4bcf-b70d-b79941e9c31a"),
                        UUID.fromString("cdcf754a-8ebd-45aa-bd0c-85719e3b16a2")
                );
    }

    // User with id 3L is the organizer of the event
    @Test
    void shouldFindEventSlotByIdWhereUserIsEitherOrganizerOrInvitedGuest() {
        UUID slotId = UUID.fromString("3075c6eb-8028-4f99-8c6c-27db1bb5cc43");
        TimeEventSlotDTO expected = TimeEventSlotDTO.builder()
                .id(slotId)
                .title("Event title")
                .startTime(LocalDateTime.parse("2024-10-11T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .location("Location")
                .description("Description")
                .organizer("kris.hudson")
                .guestEmails(Set.of("ericka.ankunding@hotmail.com"))
                .timeEventId(UUID.fromString("0c9d6398-a6de-47f0-8328-04a2f3c0511c"))
                .build();

        TimeEventSlotDTO actual = this.underTest.findByUserAndSlotId(1L, slotId);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    /*
        In this case, the event slot does not exist.
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForFindByUserAndSlotId() {
        UUID slotId = UUID.randomUUID();

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findByUserAndSlotId(
                        1L, slotId))
                .withMessage("Time event slot not found with id: " + slotId);
    }

    @Test
    void shouldFindDayEventSlotsInDateRangeWhereUserIsOrganizerOrInvitedAsGuest() {
        User user = this.userRepository.getReferenceById(2L);

        List<TimeEventSlotDTO> eventSlots = this.underTest.findEventSlotsByUserInDateRange(user,
                LocalDateTime.parse("2024-10-20T00:00:00"),
                LocalDateTime.parse("2024-10-30T00:00:00")
        );

        /*
            According to the sql script, the user has username = "clement.gulgowski" and email = "ericka.ankunding@hotmail.com"
            In the 1st event, they are invited as guest and in the 2nd, they are the organizer

            We could also assertThat(eventSlots).isSortedAccordingTo(Comparator.comparing(DayEventSlot::getStartDate))
         */
        assertThat(eventSlots).hasSize(2)
                .extracting(
                        TimeEventSlotDTO::getStartTime,
                        TimeEventSlotDTO::getGuestEmails,
                        TimeEventSlotDTO::getOrganizer)
                .containsExactly(
                        // In the 1st event UTC + 1(Europe/London) and in the 2nd one UTC + 9(Asia/Tokyo)
                        tuple(LocalDateTime.parse("2024-10-25T10:00:00"), Set.of("ericka.ankunding@hotmail.com"), "kris.hudson"),
                        tuple(LocalDateTime.parse("2024-10-28T22:00:00"), Set.of(), "clement.gulgowski"));
    }


    private TimeEvent createTimeEvent(TimeEventRequest eventRequest) {
        // We know the id from the sql script
        User user = this.userRepository.getReferenceById(1L);
        TimeEvent timeEvent = new TimeEvent();
        timeEvent.setStartTime(eventRequest.getStartTime());
        timeEvent.setEndTime(eventRequest.getEndTime());
        timeEvent.setStartTimeZoneId(eventRequest.getStartTimeZoneId());
        timeEvent.setEndTimeZoneId(eventRequest.getEndTimeZoneId());
        timeEvent.setRepetitionFrequency(eventRequest.getRepetitionFrequency());
        timeEvent.setRepetitionStep(eventRequest.getRepetitionStep());
        timeEvent.setWeeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays());
        timeEvent.setMonthlyRepetitionType(eventRequest.getMonthlyRepetitionType());
        timeEvent.setRepetitionDuration(eventRequest.getRepetitionDuration());
        timeEvent.setRepetitionEndDate(eventRequest.getRepetitionEndDate());
        timeEvent.setRepetitionOccurrences(eventRequest.getRepetitionOccurrences());
        timeEvent.setUser(user);

        this.timeEventRepository.save(timeEvent);
        this.testEntityManager.flush();

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
