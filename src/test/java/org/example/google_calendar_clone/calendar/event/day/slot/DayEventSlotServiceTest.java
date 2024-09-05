package org.example.google_calendar_clone.calendar.event.day.slot;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.calendar.event.day.DayEventRepository;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
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
@Sql(scripts = "/scripts/INIT_USERS.sql")
class DayEventSlotServiceTest extends AbstractRepositoryTest {
    @Autowired
    private DayEventSlotRepository dayEventSlotRepository;
    private DayEventSlotService underTest;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DayEventRepository dayEventRepository;
    @Autowired
    private TestEntityManager testEntityManager;
    private static final Faker FAKER = new Faker();

    @BeforeEach
    void setup() {
        underTest = new DayEventSlotService(dayEventSlotRepository);
    }

    @Test
    void shouldCreateDayEventSlotForNonRepeatingEvent() {
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-08-12"),
                LocalDate.parse("2024-08-15"),
                RepetitionFrequency.NEVER,
                null,
                null,
                null,
                null,
                null
        );
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
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-08-12"),
                LocalDate.parse("2024-08-15"),
                RepetitionFrequency.DAILY,
                10,
                null,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.parse("2024-09-12"),
                null
        );
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
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-09-04"),
                LocalDate.parse("2024-09-04"),
                RepetitionFrequency.DAILY,
                5,
                null,
                RepetitionDuration.N_REPETITIONS,
                null,
                3
        );
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
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-08-12"),
                LocalDate.parse("2024-08-15"),
                RepetitionFrequency.WEEKLY,
                2,
                null,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.parse("2024-09-10"),
                null
        );
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
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-09-04"),
                LocalDate.parse("2024-09-04"),
                RepetitionFrequency.WEEKLY,
                1,
                null,
                RepetitionDuration.N_REPETITIONS,
                null,
                4
        );
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

    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayUntilACertainDate() {
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-09-04"),
                LocalDate.parse("2024-09-04"),
                RepetitionFrequency.MONTHLY,
                1,
                MonthlyRepetitionType.SAME_DAY,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.parse("2024-12-04"),
                null
        );
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-10-04", "2024-11-04", "2024-12-04"));

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
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayForNRepetitions() {
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-09-04"),
                LocalDate.parse("2024-09-04"),
                RepetitionFrequency.MONTHLY,
                2,
                MonthlyRepetitionType.SAME_DAY,
                RepetitionDuration.N_REPETITIONS,
                null,
                3
        );
        DayEvent dayEvent = createDayEvent(request);
        List<LocalDate> dates = createDates(List.of("2024-09-04", "2024-11-04", "2025-01-04", "2025-03-04"));

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

    // 1st Wednesday of September is "2024-09-04", of October is "2024-10-02" and of November is "2024-11-06"
    @Test
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameWeekDayUntilACertainDate() {
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-09-04"),
                LocalDate.parse("2024-09-04"),
                RepetitionFrequency.MONTHLY,
                1,
                MonthlyRepetitionType.SAME_WEEKDAY,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.parse("2024-11-20"),
                null
        );
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
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-09-04"),
                LocalDate.parse("2024-09-04"),
                RepetitionFrequency.MONTHLY,
                2,
                MonthlyRepetitionType.SAME_WEEKDAY,
                RepetitionDuration.N_REPETITIONS,
                null,
                2
        );
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
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-05-18"),
                LocalDate.parse("2024-05-19"),
                RepetitionFrequency.ANNUALLY,
                1,
                null,
                RepetitionDuration.UNTIL_DATE,
                LocalDate.parse("2026-12-04"),
                null
        );
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
    void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNYearsForNRepetitions() {
        DayEventRequest request = createDayEventRequest(LocalDate.parse("2024-05-18"),
                LocalDate.parse("2024-05-19"),
                RepetitionFrequency.ANNUALLY,
                1,
                null,
                RepetitionDuration.N_REPETITIONS,
                null,
                2
        );
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

    private DayEventRequest createDayEventRequest(
            LocalDate startDate,
            LocalDate endDate,
            RepetitionFrequency repetitionFrequency,
            Integer repetitionStep,
            MonthlyRepetitionType monthlyRepetitionType,
            RepetitionDuration repetitionDuration,
            LocalDate repetitionEndDate,
            Integer repetitionCount
    ) {
        return DayEventRequest.builder()
                .name("Event name")
                .location("Location")
                .description("Description")
                .startDate(startDate)
                .endDate(endDate)
                .guestEmails(Set.of(FAKER.internet().emailAddress(), FAKER.internet().emailAddress()))
                .repetitionFrequency(repetitionFrequency)
                .repetitionStep(repetitionStep)
                .monthlyRepetitionType(monthlyRepetitionType)
                .repetitionDuration(repetitionDuration)
                .repetitionEndDate(repetitionEndDate)
                .repetitionCount(repetitionCount)
                .build();

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
