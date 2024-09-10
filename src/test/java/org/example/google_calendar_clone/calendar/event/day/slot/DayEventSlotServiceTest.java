package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.calendar.event.day.DayEventRepository;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.DayEventSlot;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/*
    Typically when we test our services we mock the repository because it is already tested. In this case, creating
    the events is not just a save() call. We need to make sure that our business logic is correct and for repeating
    events either UNTIL_DATE or for N_REPETITIONS, the day event slots are created correctly. We don't mock the
    repository and let it hit the database and fetch all the day event slots for a given day event. We can assert on the
    response.

    The findByEventId() method of the DayEventSlotRepository is also tested in this case indirectly.

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
        underTest = new DayEventSlotService(dayEventSlotRepository);
    }

    @Test
    void shouldCreateDayEventSlotForNonRepeatingEvent() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
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
                .hasName(request.getName())
                .hasLocation(request.getLocation())
                .hasDescription(request.getDescription())
                .hasGuests(request.getGuestEmails())
                .hasDayEvent(dayEvent);
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNDaysUntilACertainDate() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
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
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNDaysForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.DAILY)
                .repetitionStep(5)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(3)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-09-09", "2024-09-14", "2024-09-19"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        // Size is 4, the original event + 3 times that is to be repeated
        assertThat(dayEventSlots).hasSize(4);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNWeeksUntilACertainDate() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-08-12"))
                .endDate(LocalDate.parse("2024-08-15"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(2)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-09-10"))
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-08-12", "2024-08-26", "2024-09-09"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNWeeksForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.WEEKLY)
                .repetitionStep(1)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(4)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-09-11", "2024-09-18", "2024-09-25", "2024-10-02"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        // Size is 5, the original event + 4 times that is to be repeated
        assertThat(dayEventSlots).hasSize(5);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getName())
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
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayUntilACertainDate() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
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
                    .hasName(request.getName())
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
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2023-01-29"))
                .endDate(LocalDate.parse("2023-01-29"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_DAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(2)
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
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    // 1st Wednesday of September is "2024-09-04", of October is "2024-10-02" and of November is "2024-11-06"
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameWeekDayUntilACertainDate() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.UNTIL_DATE)
                .repetitionEndDate(LocalDate.parse("2024-11-20"))
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-10-02", "2024-11-06"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    // 1st Wednesday of September is "2024-09-04", of November is "2024-11-06", of January is "2025-01-01"
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameWeekDayForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-09-04"))
                .endDate(LocalDate.parse("2024-09-04"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(2)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(2)
                .build();
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-11-06", "2025-01-01"));

        this.underTest.create(request, dayEvent);
        this.testEntityManager.flush();

        List<DayEventSlot> dayEventSlots = this.dayEventSlotRepository.findByEventId(dayEvent.getId());

        // Size is 3, the original event + 2 times that is to be repeated
        assertThat(dayEventSlots).hasSize(3);
        for (int i = 0; i < dayEventSlots.size(); i++) {
            DayEventSlotAssert.assertThat(dayEventSlots.get(i))
                    .hasStartDate(dates.get(i))
                    .hasEndDate(dayEventSlots.get(i).getStartDate().plusDays(getEventDuration(dayEvent.getStartDate(), dayEvent.getEndDate())))
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNYearsAtUntilACertainDate() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-02-29"))
                .endDate(LocalDate.parse("2024-02-29"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
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
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNYearsForNRepetitions() {
        DayEventRequest request = DayEventRequest.builder()
                .name("Event name")
                .startDate(LocalDate.parse("2024-05-18"))
                .endDate(LocalDate.parse("2024-05-19"))
                .location("Location")
                .description("Description")
                .guestEmails(Set.of(FAKER.internet().emailAddress()))
                .repetitionFrequency(RepetitionFrequency.ANNUALLY)
                .repetitionStep(1)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(2)
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
                    .hasName(request.getName())
                    .hasLocation(request.getLocation())
                    .hasDescription(request.getDescription())
                    .hasGuests(request.getGuestEmails())
                    .hasDayEvent(dayEvent);
        }
    }

    @Test
    void shouldFindDayEventSlotsInDateRangeWhereUserIsOrganizerOrInvitedAsGuest() {
        User user = this.userRepository.getReferenceById(2L);

        List<DayEventSlotDTO> eventSlots = this.underTest.findEventSlotsByUserInDateRange(user,
                LocalDate.parse("2024-10-10"),
                LocalDate.parse("2024-10-30")
        );

        /*
            According to the sql script, the user has username = "clement.gulgowski" and email = "ericka.ankunding@hotmail.com"
            In the 1st event, they are the organizer(username) and in the 2nd, they are invited as guest

            We could also assertThat(eventSlots).isSortedAccordingTo(Comparator.comparing(DayEventSlot::getStartDate))
         */
        assertThat(eventSlots).hasSize(2)
                .extracting(
                        DayEventSlotDTO::getStartDate,
                        DayEventSlotDTO::getGuestEmails,
                        DayEventSlotDTO::getOrganizer)
                .containsExactly(
                        tuple(LocalDate.parse("2024-10-12"), Set.of(), "clement.gulgowski"),
                        tuple(LocalDate.parse("2024-10-29"), Set.of("ericka.ankunding@hotmail.com"), "ellyn.roberts"));
    }

    private DayEvent createDayEvent(DayEventRequest dayEventRequest) {
        // We know the id from the sql script
        User user = this.userRepository.getReferenceById(1L);
        DayEvent dayEvent = new DayEvent();
        dayEvent.setStartDate(dayEventRequest.getStartDate());
        dayEvent.setEndDate(dayEventRequest.getEndDate());
        dayEvent.setRepetitionFrequency(dayEventRequest.getRepetitionFrequency());
        dayEvent.setRepetitionStep(dayEventRequest.getRepetitionStep());
        dayEvent.setMonthlyRepetitionType(dayEventRequest.getMonthlyRepetitionType());
        dayEvent.setRepetitionDuration(dayEventRequest.getRepetitionDuration());
        dayEvent.setRepetitionEndDate(dayEventRequest.getRepetitionEndDate());
        dayEvent.setRepetitionCount(dayEventRequest.getRepetitionCount());
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
