package org.example.google_calendar_clone.calendar.event.slot.day;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.calendar.event.day.DayEventRepository;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.slot.day.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ConflictException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import net.datafaker.Faker;

/*
    Typically when we test our services we mock the repository because it is already tested. In this case, creating
    the events is not just a save() call. We need to make sure that our business logic is correct and for repeating
    events either UNTIL_DATE or for N_REPETITIONS, the day event slots are created correctly. We don't mock the
    repository and let it hit the database and fetch all the day event slots for a given day event. We can assert on the
    response.

    All the repository methods(our custom queries) are tested via services

    For events that are set to be repeated for FOREVER we choose an arbitrary number like 100 years and set the
    repetition End Date to plus 100 years. We treat the event then as UNTIL_DATE but now the repetitionEndDate will be
    100 years from now. Tests cover all the cases for repetition duration set to UNTIL_DATE.
 */
@Sql(scripts = {"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
class DayEventSlotServiceTest extends AbstractRepositoryTest {
    @Autowired
    private DayEventSlotRepository dayEventSlotRepository;
    @Autowired
    private DayEventRepository dayEventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager testEntityManager;
    private DayEventSlotService underTest;
    private static final Faker FAKER = new Faker();

    @BeforeEach
    void setup() {
        underTest = new DayEventSlotService(dayEventSlotRepository, dayEventRepository, userRepository);
    }

    @Test
    void shouldCreateDayEventSlotForNonRepeatingEvent() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-08-12"))
                .endDate(LocalDate.parse("2024-08-15"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.NEVER)
                .build();
        DayEvent dayEvent = createDayEvent(request);

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        // Only 1 DayEventSlot was created for a non-repeating DayEvent
        assertThat(dayEventSlots).hasSize(1);
        DayEventSlotAssert.assertThat(dayEventSlots.get(0))
                .hasStartDate(request.getStartDate())
                .hasEndDate(request.getEndDate())
                .hasName(request.getTitle())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasDayEvent(dayEvent);
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNDaysUntilDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-08-12"))
                .endDate(LocalDate.parse("2024-08-15"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(10)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-09-12"))
                .build();

        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-08-12", "2024-08-22", "2024-09-01", "2024-09-11"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(4);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNDaysForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(5)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(3)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-09-09", "2024-09-14"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    /*
        It is very important, the weeklyRecurrenceDays set to contain the day of the startDate. It is part of the
        validation. "2024-08-12" is a Monday

        "2024-08-12" is a Monday and we want the event to be repeated every 2 weeks until "2024-08-28" on Monday and
        Saturday. "2024-08-17" is the Saturday for the 1st week and on the 2nd week since Monday is at "2024-08-26"
        the next Saturday would have been "2024-08-31" but this date is after our repetition end date so is not valid.
     */
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNWeeksUntilDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-08-12"))
                .endDate(LocalDate.parse("2024-08-15"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-08-28"))
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-08-12", "2024-08-17", "2024-08-26"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    /*
        It is very important, the weeklyRecurrenceDays set to contain the day of the startDate. It is part of the
        validation. "2024-09-04" is a Wednesday

        "2024-09-04" is a Wednesday and we want the event to be repeated every 1 week, 4 times in total, on Tuesday and
        Wednesday. The 1st Tuesday in the same as week as "2024-09-04" would be "2024-09-03", but this event is in the
        past relative to our startDate, so we don't consider it.
     */
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNWeeksForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .weeklyRecurrenceDays(EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(4)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-09-10", "2024-09-11", "2024-09-17"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(4);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    /*
        This is the case where we handle events that repeat at the end of the month, where some months have 31 days
        other 30 and 28 or 29 for February if it is a leap year or not. For an event that is repeating at the same day
        and that is the last day of the month we have to adjust the upcoming events to fall on the last day of the month
        they are occurring. Below is the following example. We have an event that is to be repeated every month on the
        same day, at the 31st of January 2023 until the last day of June the 30th of the same year. The dates should be
        as follows
            "2023-01-31" => last day of January
            "2023-02-28" => last day of February for a non-leap year
            "2023-03-31" => last day of March
            "2023-04-30" => last day of April
            "2023-05-31" => last day of May
            "2023-06-30" => last day of June

        Our code handles this case gracefully.
     */
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsOnTheSameDayUntilDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2023-01-31"))
                .endDate(LocalDate.parse("2023-01-31"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2023-06-30"))
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of(
                "2023-01-31",
                "2023-02-28",
                "2023-03-31",
                "2023-04-30",
                "2023-05-31",
                "2023-06-30")
        );

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        // Size is 6, the original event + 5 times that is to be repeated
        assertThat(dayEventSlots).hasSize(6);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    /*
        This is the case where we handle events that repeat at the end of the month, where some months have 31 days
        other 30 and 28 or 29 for February if it is a leap year or not. For an event that is repeating at the same day
        every month from "2023-01-29" to "2023-03-29", we would have 3 slots. 1st one at "2023-01-29", the 2nd one since
        it is the same day it must be "2023-02-29" BUT 2023 is not a leap year, so we move the event to the last day
        of the month so the 2n slot will be at "2023-02-28" and then the last one "2023-03-29"
     */
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsOnTheSameDayForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2023-01-29"))
                .endDate(LocalDate.parse("2023-01-29"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(2)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2023-01-29", "2023-02-28", "2023-03-29"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        // Size is 4, the original event + 3 times that is to be repeated
        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    /*
        We test the edge case where a day will appear for the 5th time in some months. For example "2024-09-30" is
        the 5th Monday of September, not all months have 5 Mondays. We adjust to latest found, in our case, the last
        Monday of October is at 28, the last Monday of November is at 25 and December has 5 Mondays.
     */
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsOnTheSameWeekdayUntilDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-09-30"))
                .endDate(LocalDate.parse("2024-09-30"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-12-31"))
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-30", "2024-10-28", "2024-11-25", "2024-12-30"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(4);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    /*
        Our repetitionOccurrences are not inclusive. When it is set to 2, it will occur 2 times in total, the initial one
        plus 1 more time. Not the initial one plus the repetitionOccurrences.
        1st Wednesday of September is "2024-09-04", of November is "2024-11-06", of January is "2025-01-01"(if it was
        inclusive)
     */
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsOnTheSameWeekdayForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(2)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-11-06"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(2);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNYearsAtUntilDate() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-02-29"))
                .endDate(LocalDate.parse("2024-02-29"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2028-12-04"))
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of(
                "2024-02-29",
                "2025-02-28",
                "2026-02-28",
                "2027-02-28",
                "2028-02-29")
        );

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(5);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNYearsForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .title("Event name")
                .startDate(LocalDate.parse("2024-05-18"))
                .endDate(LocalDate.parse("2024-05-19"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionOccurrences(2)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-05-18", "2025-05-18", "2026-05-18"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getTitle())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldInviteGuests() {
        UUID slotId = UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa");
        String guestEmail = FAKER.internet().emailAddress();
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail));

        this.underTest.inviteGuests(3L, slotId, inviteGuestsRequest);
        // flush() the changes to the db so the findById() does not fetch from the cache(1st level)
        this.testEntityManager.flush();

        DayEventSlot actual = this.dayEventSlotRepository.findByIdOrThrow(slotId);
        DayEventSlotAssert.assertThat(actual)
                .hasGuests(Set.of("ericka.ankunding@hotmail.com", guestEmail));
    }

    /*
        In this case, the event slot exists the user that made the request is not the organizer
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForInviteGuests() {
        UUID slotId = UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa");
        String guestEmail = FAKER.internet().emailAddress();
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail));

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.inviteGuests(
                        2L, slotId, inviteGuestsRequest))
                .withMessage("Day event slot not found with id: " + slotId);
    }

    @Test
    void shouldThrowConflictExceptionWhenOrganizerEmailIsInGuestList() {
        UUID slotId = UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa");
        String guestEmail = FAKER.internet().emailAddress();
        // 2nd email is the email of the organizer(from the sql script)
        InviteGuestsRequest inviteGuestsRequest = new InviteGuestsRequest(Set.of(guestEmail, "waltraud.roberts@gmail.com"));

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.inviteGuests(
                        3L, slotId, inviteGuestsRequest))
                .withMessage("Organizer of the event can't be added as guest");
    }

    // The method returns the events slots in ASC order and for the given eventId we expect 4 event slots.
    @Test
    void shouldFindEventSlotsByEventId() {
        List<DayEventSlotDTO> eventSlots = this.underTest.findEventSlotsByEventId(
                UUID.fromString("4472d36c-2051-40e3-a2cf-00c6497807b5"));

        assertThat(eventSlots).hasSize(4)
                .isSortedAccordingTo(Comparator.comparing(DayEventSlotDTO::getStartDate))
                .extracting(DayEventSlotDTO::getId)
                .containsExactly(
                        UUID.fromString("5ff9cedf-ee36-4ec2-aa2e-5b6a16708ab0"),
                        UUID.fromString("009d1441-ab86-411a-baeb-77a1d976868f"),
                        UUID.fromString("35bdbe9f-9c5b-4907-8ae9-a983dacbda43"),
                        UUID.fromString("e2985eda-5c5a-40a0-851e-6dc088081afa")
                );
    }

    // User with id 3L is the organizer of the event
    @Test
    void shouldFindEventSlotByIdWhereUserIsEitherOrganizerOrInvitedGuest() {
        UUID slotId = UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa");
        DayEventSlotDTO expected = DayEventSlotDTO.builder()
                .id(slotId)
                .title("Event title")
                .startDate(LocalDate.parse("2024-10-29"))
                .endDate(LocalDate.parse("2024-10-30"))
                .location("Location")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of("ericka.ankunding@hotmail.com"))
                .dayEventId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"))
                .build();

        DayEventSlotDTO actual = this.underTest.findByUserAndSlotId(3L, slotId);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    /*
        In this case, the event slot does not exist.
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForFindByUserAndSlotId() {
        UUID slotId = UUID.randomUUID();

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findByUserAndSlotId(
                        3L, slotId))
                .withMessage("Day event slot not found with id: " + slotId);
    }

    @Test
    void shouldFindDayEventSlotsInDateRangeWhereUserIsOrganizerOrInvitedAsGuest() {
        // We need both the id and the email, we can not call getReferenceById(). The proxy will not have the email,
        // and when we call user.getEmail() it will initialize the proxy by querying the db.
        User user = this.userRepository.findAuthUserByIdOrThrow(2L);

        List<DayEventSlotDTO> eventSlots = this.underTest.findEventSlotsByUserInDateRange(user,
                LocalDate.parse("2024-10-10"),
                LocalDate.parse("2024-10-30")
        );

        /*
            According to the sql script, the user has username = "clement.gulgowski" and email = "ericka.ankunding@hotmail.com"
            In the 1st event, they are the organizer(username) and in the 2nd, they are invited as guest

            We could also assertThat(eventSlots).isSortedAccordingTo(Comparator.comparing(DayEventSlotDTO::getStartDate))
         */
        assertThat(eventSlots).hasSize(2)
                .extracting(
                        DayEventSlotDTO::getId,
                        DayEventSlotDTO::getStartDate,
                        DayEventSlotDTO::getGuestEmails,
                        DayEventSlotDTO::getOrganizer)
                .containsExactly(
                        tuple(UUID.fromString("e2985eda-5c5a-40a0-851e-6dc088081afa"),
                                LocalDate.parse("2024-10-12"),
                                Set.of(),
                                "clement.gulgowski"),
                        tuple(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"),
                                LocalDate.parse("2024-10-29"),
                                Set.of("ericka.ankunding@hotmail.com"),
                                "ellyn.roberts"));
    }

    private DayEvent createDayEvent(DayEventRequest dayEventRequest) {
        // We know the id from the sql script
        User user = this.userRepository.getReferenceById(1L);
        DayEvent dayEvent = new DayEvent();
        dayEvent.setStartDate(dayEventRequest.getStartDate());
        dayEvent.setEndDate(dayEventRequest.getEndDate());
        dayEvent.setRepetitionFrequency(dayEventRequest.getRepetitionFrequency());
        dayEvent.setRepetitionStep(dayEventRequest.getRepetitionStep());
        dayEvent.setWeeklyRecurrenceDays(dayEventRequest.getWeeklyRecurrenceDays());
        dayEvent.setMonthlyRepetitionType(dayEventRequest.getMonthlyRepetitionType());
        dayEvent.setRepetitionDuration(dayEventRequest.getRepetitionDuration());
        dayEvent.setRepetitionEndDate(dayEventRequest.getRepetitionEndDate());
        dayEvent.setRepetitionOccurrences(dayEventRequest.getRepetitionOccurrences());
        dayEvent.setUser(user);

        this.dayEventRepository.save(dayEvent);
        this.testEntityManager.flush();

        return dayEvent;
    }

    /*
        The list of dates is returned in ascending order, representing the upcoming occurrences of the event.
        We retrieve the day event slots ordered by their start date. The i-th date in the list should match
        the start date of the i-th event slot if the dates are computed correctly.

        For each case, the values passed to createDates() represent the exact dates we expect the events to occur.
     */
    private List<LocalDate> createDates(List<String> dates) {
        return dates.stream()
                .map(LocalDate::parse)
                .toList();
    }

    private int getEventDuration(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }
}
