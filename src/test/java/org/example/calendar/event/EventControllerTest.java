package org.example.calendar.event;

import org.example.calendar.event.day.DayEventService;
import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.event.recurrence.MonthlyRecurrenceType;
import org.example.calendar.event.recurrence.RecurrenceDuration;
import org.example.calendar.event.recurrence.RecurrenceFrequency;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.time.TimeEventService;
import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.example.calendar.config.SecurityConfig;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.AuthTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
    We can not hardcode dates, because eventually there will be in the past and our validation requires the dates to
    be in the future or present. We create them dynamically relative to now().
 */
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
    private TimeEventService timeEventService;
    @MockBean
    private DayEventService dayEventService;
    private static final String DAY_EVENT_PATH = "/api/v1/events/day-events";
    private static final String TIME_EVENT_PATH = "/api/v1/events/time-events";

    // createDayEvent()
    @Test
    void should201WhenDayEventIsCreatedSuccessfully() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        UUID eventId = UUID.randomUUID();

        when(this.dayEventService.createEvent(1L, eventRequest)).thenReturn(eventId);

        this.mockMvc.perform(post(DAY_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(DAY_EVENT_PATH + "/" + eventId))
                );
    }

    // createDayEvent() @Valid
    @Test
    void should400WhenDayEventRequestStartDateIsAfterEndDate() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(3), LocalDate.now().plusDays(1));
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
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // createDayEvent()
    @Test
    void should401WhenCreateDayEventIsCalledByUnauthenticatedUser() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
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
                        .content(this.objectMapper.writeValueAsString(eventRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // createDayEvent()
    @Test
    void should403WhenCreateDayEventIsCalledWithNoCsrf() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
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
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.dayEventService);
    }

    // createDayEvent()
    @Test
    void should403WhenCreateDayEventIsCalledWithInvalidCsrf() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
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
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.dayEventService);
    }

    // updateDayEvent()
    @Test
    void should204WhenDayEventIsUpdatedSuccessfully() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        UUID eventId = UUID.randomUUID();

        doNothing().when(this.dayEventService).updateEvent(1L, eventId, eventRequest);

        this.mockMvc.perform(put(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.dayEventService, times(1)).updateEvent(1L, eventId, eventRequest);
    }

    // updateDayEvent()
    @Test
    void should401WhenUpdateDayEventIsCalledByUnauthenticatedUser() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);

        this.mockMvc.perform(put(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody));

        verifyNoInteractions(this.dayEventService);
    }

    // updateDayEvent()
    @Test
    void should403WhenUpdateDayEventIsCalledWithNoCsrf() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);

        this.mockMvc.perform(put(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody));

        verifyNoInteractions(this.dayEventService);
    }

    // updateDayEvent()
    @Test
    void should403WhenUpdateDayEventIsCalledWithInvalidCsrf() throws Exception {
        DayEventRequest eventRequest = createDayEventRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);

        this.mockMvc.perform(put(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody));

        verifyNoInteractions(this.dayEventService);
    }

    // findDayEventSlotsByEventId()
    @Test
    void should200WithListOfDayEventSlotDTO() throws Exception {
        UUID eventId = UUID.randomUUID();
        DayEventSlotPublicProjection dayEventSlotPublicProjection = createDayEventSlotPublicProjection(eventId);

        when(this.dayEventService.findEventSlotsByEventId(eventId, 1L)).thenReturn(List.of(dayEventSlotPublicProjection));

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(List.of(dayEventSlotPublicProjection)))
                );
    }

    // findDayEventSlotsByEventId()
    @Test
    void should404WhenDayEventIsNotFoundForFindDayEventSlotsByEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, DAY_EVENT_PATH, eventId);

        when(this.dayEventService.findEventSlotsByEventId(eventId, 1L)).thenThrow(new ResourceNotFoundException("Day event not found with id: " + eventId));

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // findDayEventSlotsByEventId()
    @Test
    void should404WhenUserIsNotOrganizerOfDayEventForFindDayEventSlotsByEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, DAY_EVENT_PATH, eventId);

        when(this.dayEventService.findEventSlotsByEventId(eventId, 1L)).thenThrow(new ResourceNotFoundException("Day event not found with id: " + eventId));

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // findDayEventSlotsByEventId()
    @Test
    void should401WhenFindDayEventSlotsByIdIsCalledByUnauthenticatedUser() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);

        this.mockMvc.perform(get(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // deleteDayEventById()
    @Test
    void should204WhenDayEventIsDeletedSuccessfully() throws Exception {
        UUID eventId = UUID.randomUUID();
        doNothing().when(this.dayEventService).deleteEventById(eventId, 1L);

        this.mockMvc.perform(delete(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.dayEventService, times(1)).deleteEventById(eventId, 1L);
    }

    // deleteDayEventById()
    @Test
    void should404WhenDayEventIsNotFoundForDeleteDayEventById() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, DAY_EVENT_PATH, eventId);

        doThrow(new ResourceNotFoundException("Day event not found with id: " + eventId)).when(this.dayEventService).deleteEventById(eventId, 1L);

        this.mockMvc.perform(delete(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // deleteDayEventById()
    @Test
    void should404WhenUserIsNotOrganizerOfDayEventForDeleteDayEventById() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, DAY_EVENT_PATH, eventId);

        doThrow(new ResourceNotFoundException("Day event not found with id: " + eventId)).when(this.dayEventService).deleteEventById(eventId, 1L);

        this.mockMvc.perform(delete(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // deleteDayEventById()
    @Test
    void should401WhenDeleteDayEventByIdIsCalledByUnauthenticatedUser() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);

        this.mockMvc.perform(delete(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // deleteDayEventById()
    @Test
    void should403WhenDeleteDayEventByIdIsCalledWithNoCsrf() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);


        this.mockMvc.perform(delete(DAY_EVENT_PATH + "/{eventId}", eventId)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
    }

    // deleteDayEventById()
    @Test
    void should403WhenDeleteDayEventByIdIsCalledWithInvalidCsrf() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_PATH, eventId);


        this.mockMvc.perform(delete(DAY_EVENT_PATH + "/{eventId}", eventId).with(csrf().useInvalidToken().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.dayEventService);
    }

    // createTimeEvent()
    @Test
    void should201WhenTimeEventIsCreatedSuccessfully() throws Exception {
        TimeEventRequest timeEventRequest = createTimeEventRequest(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30)
        );
        UUID eventId = UUID.randomUUID();

        when(this.timeEventService.createEvent(1L, timeEventRequest)).thenReturn(eventId);

        this.mockMvc.perform(post(TIME_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(timeEventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isCreated(),
                        header().string("Location", containsString(TIME_EVENT_PATH + "/" + eventId))
                );
    }

    // createTimeEvent() @Valid
    @Test
    void should400WhenTimeEventRequestStartTimeIsAfterEndTime() throws Exception {
        TimeEventRequest timeEventRequest = createTimeEventRequest(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1));
        String responseBody = String.format("""
                {
                    "status": 400,
                    "type": "BAD_REQUEST",
                    "message": "Start time must be before end time",
                    "path": "%s"
                }
                """, TIME_EVENT_PATH);


        this.mockMvc.perform(post(TIME_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(timeEventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isBadRequest(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // createTimeEvent()
    @Test
    void should401WhenCreateTimeEventIsCalledByUnauthenticatedUser() throws Exception {
        TimeEventRequest timeEventRequest = createTimeEventRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3));
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s"
                }
                """, TIME_EVENT_PATH);


        this.mockMvc.perform(post(TIME_EVENT_PATH).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(timeEventRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // createTimeEvent()
    @Test
    void should403WhenCreateTimeEventIsCalledWithNoCsrf() throws Exception {
        TimeEventRequest timeEventRequest = createTimeEventRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3));
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s"
                }
                """, TIME_EVENT_PATH);


        this.mockMvc.perform(post(TIME_EVENT_PATH)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(timeEventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // createTimeEvent()
    @Test
    void should403WhenCreateTimeEventIsCalledWithInvalidCsrf() throws Exception {
        TimeEventRequest timeEventRequest = createTimeEventRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3));
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s"
                }
                """, TIME_EVENT_PATH);


        this.mockMvc.perform(post(TIME_EVENT_PATH).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(timeEventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // updateTimeEvent()
    @Test
    void should204WhenTimEventIsUpdatedSuccessfully() throws Exception {
        TimeEventRequest eventRequest = createTimeEventRequest(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30)
        );
        UUID eventId = UUID.randomUUID();

        doNothing().when(this.timeEventService).updateEvent(1L, eventId, eventRequest);

        this.mockMvc.perform(put(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.timeEventService, times(1)).updateEvent(1L, eventId, eventRequest);
    }

    // updateTimeEvent()
    @Test
    void should401WhenUpdateTimeEventIsCalledByUnauthenticatedUser() throws Exception {
        TimeEventRequest eventRequest = createTimeEventRequest(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30)
        );
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);

        this.mockMvc.perform(put(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // updateTimeEvent()
    @Test
    void should403WhenUpdateTimeEventIsCalledWithNoCsrf() throws Exception {
        TimeEventRequest eventRequest = createTimeEventRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);


        this.mockMvc.perform(put(TIME_EVENT_PATH + "/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // updateTimeEvent()
    @Test
    void should403WhenUpdateTimeEventIsCalledWithInvalidCsrf() throws Exception {
        TimeEventRequest eventRequest = createTimeEventRequest(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30));
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);


        this.mockMvc.perform(put(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventService);
    }

    // findTimeEventSlotsByEventId()
    @Test
    void should200WithListOfTimeEventSlotDTO() throws Exception {
        UUID eventId = UUID.randomUUID();
        TimeEventSlotPublicProjection timeEventSlotDTO = createTimeEventSlotPublicProjection(eventId);

        when(this.timeEventService.findEventSlotsByEventId(eventId, 1L)).thenReturn(List.of(timeEventSlotDTO));

        this.mockMvc.perform(get(TIME_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(List.of(timeEventSlotDTO)))
                );
    }

    // findTimeEventSlotsByEventId()
    @Test
    void should404WhenTimeEventIsNotFoundForFindTimeEventSlotsByEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, TIME_EVENT_PATH, eventId);

        when(this.timeEventService.findEventSlotsByEventId(eventId, 1L)).thenThrow(new ResourceNotFoundException("Time event not found with id: " + eventId));

        this.mockMvc.perform(get(TIME_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // findTimeEventSlotsByEventId()
    @Test
    void should404WhenUserIsNotOrganizerOfTimeEventForFindTimeEventSlotsByEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, TIME_EVENT_PATH, eventId);

        when(this.timeEventService.findEventSlotsByEventId(eventId, 1L)).thenThrow(new ResourceNotFoundException("Time event not found with id: " + eventId));

        this.mockMvc.perform(get(TIME_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // findTimeEventSlotsByEventId()
    @Test
    void should401WhenFindTimeEventSlotsByEventIdIsCalledByUnauthenticatedUser() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);

        this.mockMvc.perform(get(TIME_EVENT_PATH + "/{eventId}", eventId)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.timeEventService);
    }

    // deleteTimeEventById()
    @Test
    void should204WhenTimeEventIsDeletedSuccessfully() throws Exception {
        UUID eventId = UUID.randomUUID();
        doNothing().when(this.timeEventService).deleteEventById(eventId, 1L);

        this.mockMvc.perform(delete(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.timeEventService, times(1)).deleteEventById(eventId, 1L);
    }

    // deleteTimeEventById()
    @Test
    void should404WhenTimeEventIsNotFoundForDeleteDayEventById() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, TIME_EVENT_PATH, eventId);

        doThrow(new ResourceNotFoundException("Time event not found with id: " + eventId)).when(this.timeEventService).deleteEventById(eventId, 1L);

        this.mockMvc.perform(delete(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // deleteTimeEventById()
    @Test
    void should404WhenUserIsNotOrganizerOfTimeEventForDeleteDayEventById() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event not found with id: %s",
                    "path": "%s/%s"
                }
                """, eventId, TIME_EVENT_PATH, eventId);

        doThrow(new ResourceNotFoundException("Time event not found with id: " + eventId)).when(this.timeEventService).deleteEventById(eventId, 1L);

        this.mockMvc.perform(delete(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // deleteTimeEventById()
    @Test
    void should401WhenDeleteTimeEventByIdIsCalledByUnauthenticatedUser() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);

        this.mockMvc.perform(delete(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().asHeader())
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.timeEventService);
    }

    // deleteTimeEventById()
    @Test
    void should403WhenDeleteTimeEventByIdIsCalledWithNoCsrf() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);


        this.mockMvc.perform(delete(TIME_EVENT_PATH + "/{eventId}", eventId)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.timeEventService);
    }

    // deleteTimeEventById()
    @Test
    void should403WhenDeleteTimeEventByIdIsCalledWithInvalidCsrf() throws Exception {
        UUID eventId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_PATH, eventId);


        this.mockMvc.perform(delete(TIME_EVENT_PATH + "/{eventId}", eventId).with(csrf().useInvalidToken().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.timeEventService);
    }

    // findEventsByUserInDateRange()
    @Test
    void should200WithListOfEventSlotPublicProjection() throws Exception {
        DayEventSlotPublicProjection dayEventSlotPublicProjection = createDayEventSlotPublicProjection(UUID.randomUUID());
        TimeEventSlotPublicProjection timeEventSlotPublicProjection = createTimeEventSlotPublicProjection(UUID.randomUUID());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(4);

        when(this.dayEventService.findEventSlotsByUserInDateRange(1L, startDate, endDate)).thenReturn(List.of(dayEventSlotPublicProjection));
        when(this.timeEventService.findEventSlotsByUserInDateRange(1L, startDate.atStartOfDay(), ZoneId.of("Australia/Sydney"), endDate.atStartOfDay(), ZoneId.of("Australia/Sydney"))).thenReturn(List.of(timeEventSlotPublicProjection));

        this.mockMvc.perform(get("/api/v1/events?start={start}&end={end}&startTimeZoneId={startTimeZoneId}&endTimeZoneId={endTimeZoneId}", LocalDate.now(), LocalDate.now().plusDays(4), "Australia/Sydney", "Australia/Sydney")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(List.of(dayEventSlotPublicProjection, timeEventSlotPublicProjection)))
                );
    }
    @Test
    void should401WhenFindEventSlotsByUserInDateRangeIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "/api/v1/events"
                }
                """;

        this.mockMvc.perform(get("/api/v1/events?start={start}&end={end}", LocalDate.now(), LocalDate.now().plusDays(4))
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventService);
        verifyNoInteractions(this.timeEventService);
    }

    private DayEventRequest createDayEventRequest(LocalDate startDate, LocalDate endDate) {
        return DayEventRequest.builder()
                .title("Event name")
                .location("Location")
                .description("Description")
                .startDate(startDate)
                .endDate(endDate)
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(3)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_WEEKDAY)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(3)
                .build();
    }

    private DayEventSlotPublicProjection createDayEventSlotPublicProjection(UUID eventId) {
        return DayEventSlotPublicProjection.builder()
                .id(UUID.fromString("367be25c-fd30-4961-8d03-81b80a765c3e"))
                .title("Event title")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(3))
                .location("Location")
                .description("Description")
                .organizer("Organizer")
                .guestEmails(Set.of())
                .eventId(eventId)
                .build();
    }


    private TimeEventRequest createTimeEventRequest(LocalDateTime startTime, LocalDateTime endTime) {
        return TimeEventRequest.builder()
                .title("Event title")
                .location("Location")
                .description("Description")
                .startTime(startTime)
                .endTime(endTime)
                .startTimeZoneId(ZoneId.of("Australia/Sydney"))
                .endTimeZoneId(ZoneId.of("Australia/Sydney"))
                .recurrenceFrequency(RecurrenceFrequency.MONTHLY)
                .recurrenceStep(3)
                .monthlyRecurrenceType(MonthlyRecurrenceType.SAME_WEEKDAY)
                .recurrenceDuration(RecurrenceDuration.N_OCCURRENCES)
                .numberOfOccurrences(3)
                .build();
    }

    private TimeEventSlotPublicProjection createTimeEventSlotPublicProjection(UUID eventId) {
        return TimeEventSlotPublicProjection.builder()
                .id(UUID.fromString("367be25c-fd30-4961-8d03-81b80a765c3e"))
                .title("Event name")
                .startTime(LocalDateTime.now(ZoneId.of("Australia/Sydney")).plusDays(1))
                .endTime(LocalDateTime.now(ZoneId.of("Australia/Sydney")).plusDays(3))
                .startTimeZoneId(ZoneId.of("Australia/Sydney"))
                .endTimeZoneId(ZoneId.of("Australia/Sydney"))
                .location("Location")
                .description("Description")
                .organizer("Organizer")
                .guestEmails(Set.of())
                .eventId(eventId)
                .build();
    }
}
