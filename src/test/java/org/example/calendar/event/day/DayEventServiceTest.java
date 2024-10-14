package org.example.calendar.event.day;

import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.email.EmailService;
import org.example.calendar.entity.User;
import org.example.calendar.event.day.dto.DayEventInvitationRequest;
import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.slot.day.DayEventSlotPublicProjectionAssert;
import org.example.calendar.event.slot.day.DayEventSlotRepository;
import org.example.calendar.event.slot.day.DayEventSlotService;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
    We test the repository via the service. We can not use @InjectMocks because the properties of the class are not
    all mocks. The methods findEventSlotsByEventId() and findEventSlotsByUserInDateRange() are tested indirectly for
    the assertions
 */
@ExtendWith(MockitoExtension.class)
@Import({DayEventRepository.class, DayEventSlotRepository.class, UserRepository.class})
class DayEventServiceTest extends AbstractRepositoryTest {
    @Autowired
    private DayEventRepository eventRepository;
    @Autowired
    private DayEventSlotRepository eventSlotRepository;
    @Autowired
    private UserRepository userRepository;
    private DayEventSlotService dayEventSlotService;
    @Mock
    private EmailService emailService;
    private DayEventService underTest;

    @BeforeEach
    void setup() {
        this.dayEventSlotService = new DayEventSlotService(eventSlotRepository, userRepository);
        this.underTest = new DayEventService(dayEventSlotService, eventRepository, userRepository, emailService);
    }

    @Test
    @Sql("/scripts/INIT_USERS.sql")
    void shouldCreateDayEvent() {
        User user = User.builder()
                .id(1L)
                .username("kris.hudson")
                .email("joshua.wolf@hotmail.com")
                .build();
        DayEventRequest eventRequest = DayEventRequest.builder()
                .title("Event title")
                .location("Location")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .recurrenceFrequency(RecurrenceFrequency.NEVER)
                .guestEmails(Collections.emptySet())
                .build();
        // We don't have to call any(DayEventInvitationRequest.class) for the email service. We know exactly how that request will look like
        // and can verify that our email service is called with the expected email request
        DayEventInvitationRequest emailRequest = DayEventInvitationRequest.builder()
                .eventName(eventRequest.getTitle())
                .organizer(user.getUsername())
                .location(eventRequest.getLocation())
                .description(eventRequest.getDescription())
                .guestEmails(eventRequest.getGuestEmails())
                .recurrenceFrequency(eventRequest.getRecurrenceFrequency())
                .recurrenceStep(eventRequest.getRecurrenceStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRecurrenceType(eventRequest.getMonthlyRecurrenceType())
                .recurrenceDuration(eventRequest.getRecurrenceDuration())
                .recurrenceEndDate(eventRequest.getRecurrenceEndDate())
                .numbersOfOccurrences(eventRequest.getNumberOfOccurrences())
                .startDate(eventRequest.getStartDate())
                .build();
        doNothing().when(this.emailService).sendInvitationEmail(emailRequest);

        UUID eventId = this.underTest.createEvent(1L, eventRequest);

        // We can not call findEventSlotsByEventId, because we don't have access to the auto generate event id
        List<DayEventSlotPublicProjection> projections = this.dayEventSlotService.findEventSlotsByUserInDateRange(user, LocalDate.now(), LocalDate.now().plusDays(2));

        assertThat(eventId).isNotNull();
        DayEventSlotPublicProjectionAssert.assertThat(projections.get(0))
                .hasStartDate(LocalDate.now().plusDays(1))
                .hasEndDate(LocalDate.now().plusDays(1))
                .hasTitle("Event title")
                .hasLocation("Location")
                .hasDescription(null)
                .hasGuests(Collections.emptySet());

        verify(this.emailService, times(1)).sendInvitationEmail(emailRequest);
    }

    /*
        In this method, the event request we provide has different frequency properties and start/end date from the
        current state of the resource(event). In this case, we delete all the event slots(4) and compute the new ones
        based on the event request.
     */
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldUpdateEvent_1() {
        UUID eventId = UUID.fromString("4472d36c-2051-40e3-a2cf-00c6497807b5");
        DayEventRequest eventRequest = DayEventRequest.builder()
                .location("Location")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .recurrenceFrequency(RecurrenceFrequency.NEVER)
                .guestEmails(Collections.emptySet())
                .build();

        this.underTest.updateEvent(2L, eventId, eventRequest);

        List<DayEventSlotPublicProjection> projections = this.dayEventSlotService.findEventSlotsByEventAndUserId(eventId, 2L);

        assertThat(projections).hasSize(1);
        DayEventSlotPublicProjectionAssert.assertThat(projections.get(0))
                .hasStartDate(LocalDate.now().plusDays(1))
                .hasEndDate(LocalDate.now().plusDays(1))
                .hasTitle(null)
                .hasLocation("Location")
                .hasDescription(null)
                .hasGuests(Collections.emptySet())
                .hasEventId(eventId);
    }

    /*
        In this method, the event request we provide has the same frequency properties and start/end date with the
        current state of the resource(event). In this case, we update title/location/description/guestEmails to all of
        the event's slots(4).
     */
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldUpdateEvent_2() {
        UUID eventId = UUID.fromString("4472d36c-2051-40e3-a2cf-00c6497807b5");
        DayEventRequest eventRequest = DayEventRequest.builder()
                .location("Location")
                .startDate(LocalDate.parse("2024-09-30"))
                .endDate(LocalDate.parse("2024-09-30"))
                .recurrenceFrequency(RecurrenceFrequency.DAILY)
                .recurrenceStep(4)
                .recurrenceDuration(RecurrenceDuration.UNTIL_DATE)
                .recurrenceEndDate(LocalDate.parse("2024-10-14"))
                .guestEmails(Collections.emptySet())
                .build();

        this.underTest.updateEvent(2L, eventId, eventRequest);

        List<DayEventSlotPublicProjection> projections = this.dayEventSlotService.findEventSlotsByEventAndUserId(eventId, 2L);

        assertThat(projections).hasSize(4);
        projections.forEach(projection -> DayEventSlotPublicProjectionAssert.assertThat(projections.get(0))
                .hasTitle(null)
                .hasLocation("Location")
                .hasDescription(null)
                .hasGuests(Collections.emptySet())
                .hasEventId(eventId)
        );
    }

    /*
        There are 2 cases where the findByEventIdAndUserId() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
        In either case it is 404
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForUpdateEvent() {
        DayEventRequest eventRequest = DayEventRequest.builder()
                .title("Event title")
                .build();
        UUID eventId = UUID.randomUUID();

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.updateEvent(2L, eventId, eventRequest)).withMessage("Day event not found with id: " + eventId);
    }

    // We can also assert that findByEventSlotsByEventId returns an empty list. Event slots are delete for the event with ON DELETE CASCADE
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_EVENTS.sql"})
    void shouldDeleteEvent() {
        UUID eventId = UUID.fromString("4472d36c-2051-40e3-a2cf-00c6497807b5");
        this.underTest.deleteEventById(eventId, 2L);

        // 0 rows affected
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.deleteEventById(eventId, 1L)).withMessage("Day event not found with id: " + eventId);
    }

    /*
        There are 2 cases where the deleteEventById() could throw ResourceNotFoundException.
            1. Event exists but the authenticated user is not the organizer
            2. Event does not exist
         In either case it is 404
     */
    @Test
    void shouldThrowResourceNotFoundExceptionForDeleteById() {
        UUID eventId = UUID.randomUUID();
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.deleteEventById(eventId, 1L)).withMessage("Day event not found with id: " + eventId);
    }
}
