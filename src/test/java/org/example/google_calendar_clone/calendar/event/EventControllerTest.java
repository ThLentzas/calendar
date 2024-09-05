package org.example.google_calendar_clone.calendar.event;

import org.example.google_calendar_clone.calendar.event.day.DayEventService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.repetition.MonthlyRepetitionType;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.config.SecurityConfig;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.utils.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private JwtDecoder jwtDecoder;
    @MockBean
    private DayEventService dayEventService;
    private static final String DAY_EVENT_PATH = "/api/v1/events/day-events";

    // createDayEvent()
    @Test
    void should201WhenDayEventIsCreatedSuccessfully() throws Exception {
        DayEventRequest dayEventRequest = createDayEventRequest("2024-10-11", "2024-10-15");
        DayEvent dayEvent = createDayEvent(dayEventRequest);

        when(this.dayEventService.create(any(Jwt.class), eq(dayEventRequest))).thenReturn(dayEvent);

        this.mockMvc.perform(post(DAY_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dayEventRequest))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(DAY_EVENT_PATH + "/" + dayEvent.getId()))
                );
    }

    // createDayEvent() @Valid
    @Test
    void should400WhenDayEventRequestStartDateIsAfterEndDate() throws Exception {
        DayEventRequest dayEventRequest = createDayEventRequest("2024-10-15", "2024-10-11");
        String responseBody = String.format("""
                {
                    "status": 400,
                    "type": "BAD_REQUEST",
                    "message": "Start date must be before end date",
                    "path": "%s"
                }
                """, DAY_EVENT_PATH);


        this.mockMvc.perform(post(DAY_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dayEventRequest))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // createDayEvent()
    @Test
    void should401WhenCreateDayEventIsCalledByUnauthorizedUser() throws Exception {
        DayEventRequest dayEventRequest = createDayEventRequest("2024-10-11", "2024-10-15");
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s"
                }
                """, DAY_EVENT_PATH);


        this.mockMvc.perform(post(DAY_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dayEventRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // createDayEvent()
    @Test
    void should403WhenCreateDayEventIsCalledWithNoCsrf() throws Exception {
        DayEventRequest dayEventRequest = createDayEventRequest("2024-10-11", "2024-10-15");
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s"
                }
                """, DAY_EVENT_PATH);


        this.mockMvc.perform(post(DAY_EVENT_PATH)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dayEventRequest))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.dayEventService);
    }

    // createDayEvent()
    @Test
    void should403WhenCreateDayEventIsCalledWithInvalidCsrf() throws Exception {
        DayEventRequest dayEventRequest = createDayEventRequest("2024-10-11", "2024-10-15");
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s"
                }
                """, DAY_EVENT_PATH);


        this.mockMvc.perform(post(DAY_EVENT_PATH).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dayEventRequest))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.dayEventService);
    }

    // findDayEventSlotsByEventId()
    @Test
    void should200WithListOfDayEventSlotDTO() throws Exception {
        UUID eventId = UUID.randomUUID();
        DayEventSlotDTO dayEventSlotDTO = DayEventSlotDTO.builder()
                .id(UUID.fromString("367be25c-fd30-4961-8d03-81b80a765c3e"))
                .name("Event name")
                .startDate(LocalDate.parse("2024-10-11"))
                .endDate(LocalDate.parse("2024-10-15"))
                .location("Location")
                .description("Description")
                .organizer("Organizer")
                .guestEmails(Set.of())  // Assuming an empty set for guest emails
                .build();

        when(this.dayEventService.findEventSlotsByEventId(any(Jwt.class), eq(eventId))).thenReturn(List.of(dayEventSlotDTO));

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(List.of(dayEventSlotDTO)))
                );
    }

    @Test
    void should404WhenDayEventIsNotFoundForFindDayEventSlotsByEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event not found with id: %s",
                    "path": "/api/v1/events/day-events/%s"
                }
                """, eventId, eventId);

        when(this.dayEventService.findEventSlotsByEventId(any(Jwt.class), eq(eventId))).thenThrow(
                new ResourceNotFoundException("Day event not found with id: " + eventId));

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    @Test
    void should403WhenCurrentUserIsNotOrganizerOfRequestedDayEventForFindDayEventSlotsByEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/events/day-events/%s"
                }
                """, eventId);

        when(this.dayEventService.findEventSlotsByEventId(any(Jwt.class), eq(eventId))).thenThrow(
                new AccessDeniedException("Access Denied"));

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
    }

    private DayEventRequest createDayEventRequest(String startDate, String endDate) {
        return DayEventRequest.builder()
                .name("Event name")
                .location("Location")
                .description("Description")
                .startDate(LocalDate.parse(startDate))
                .endDate(LocalDate.parse(endDate))
                .repetitionFrequency(RepetitionFrequency.MONTHLY)
                .repetitionStep(3)
                .monthlyRepetitionType(MonthlyRepetitionType.SAME_WEEKDAY)
                .repetitionDuration(RepetitionDuration.N_REPETITIONS)
                .repetitionCount(3)
                .build();
    }

    private DayEvent createDayEvent(DayEventRequest dayEventRequest) {
        DayEvent dayEvent = new DayEvent();
        dayEvent.setId(UUID.randomUUID());
        dayEvent.setStartDate(dayEventRequest.getStartDate());
        dayEvent.setEndDate(dayEventRequest.getEndDate());
        dayEvent.setRepetitionFrequency(dayEventRequest.getRepetitionFrequency());
        dayEvent.setRepetitionStep(dayEventRequest.getRepetitionStep());
        dayEvent.setMonthlyRepetitionType(dayEventRequest.getMonthlyRepetitionType());
        dayEvent.setRepetitionDuration(dayEventRequest.getRepetitionDuration());
        dayEvent.setRepetitionEndDate(dayEventRequest.getRepetitionEndDate());
        dayEvent.setRepetitionCount(dayEventRequest.getRepetitionCount());

        return dayEvent;
    }
}
