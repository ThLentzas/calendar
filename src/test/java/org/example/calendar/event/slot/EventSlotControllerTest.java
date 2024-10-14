package org.example.calendar.event.slot;


import org.example.calendar.AuthTestUtils;
import org.example.calendar.event.dto.InviteGuestsRequest;
import org.example.calendar.event.slot.day.DayEventSlotService;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.slot.day.dto.DayEventSlotRequest;
import org.example.calendar.event.slot.time.TimeEventSlotService;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.example.calendar.event.slot.time.dto.TimeEventSlotRequest;
import org.example.calendar.config.SecurityConfig;
import org.example.calendar.exception.ConflictException;
import org.example.calendar.exception.ResourceNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;

@WebMvcTest(EventSlotController.class)
@Import(SecurityConfig.class)
class EventSlotControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtDecoder jwtDecoder;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DayEventSlotService dayEventSlotService;
    @MockBean
    private TimeEventSlotService timeEventSlotService;
    private static final Faker FAKER = new Faker();
    private static final String DAY_EVENT_SLOT_PATH = "/api/v1/event-slots/day-event-slots";
    private static final String TIME_EVENT_SLOT_PATH = "/api/v1/event-slots/time-event-slots";

    // inviteGuestsToDayEventSlot
    @Test
    void should204WhenInviteGuestsToDayEventSlotIsSuccessfulForDayEventSlot() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();

        doNothing().when(this.dayEventSlotService).inviteGuests(1L, slotId, request);

        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.dayEventSlotService, times(1)).inviteGuests(1L, slotId, request);
    }

    // inviteGuestsToDayEventSlot
    @Test
    void should404WhenDayEventSlotDoesNotExistForInviteGuestsToDayEventSlot() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event slot not found with id: %s",
                    "path": "%s/%s/invite"
                }
                """, slotId, DAY_EVENT_SLOT_PATH, slotId);


        doThrow(new ResourceNotFoundException("Day event slot not found with id: " + slotId)).when(this.dayEventSlotService).inviteGuests(1L, slotId, request);

        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );

        verify(this.dayEventSlotService, times(1)).inviteGuests(1L, slotId, request);
    }

    // inviteGuestsToDayEventSlot
    @Test
    void should409WhenOrganizerEmailIsInGuestListForInviteGuestsToDayEventSlot() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 409,
                    "type": "CONFLICT",
                    "message": "Organizer of the event can't be added as guest",
                    "path": "%s/%s/invite"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        doThrow(new ConflictException("Organizer of the event can't be added as guest")).when(this.dayEventSlotService).inviteGuests(1L, slotId, request);

        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody, false)
                );

        verify(this.dayEventSlotService, times(1)).inviteGuests(1L, slotId, request);
    }

    // inviteGuestsToDayEventSlot
    @Test
    void should401WhenInviteGuestsToDayEventSlotIsCalledByUnauthenticatedUser() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s/invite"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // inviteGuestsToDayEventSlot
    @Test
    void should403WhenInviteGuestsToDayEventSlotIsCalledWithNoCsrf() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s/invite"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // inviteGuestsToDayEventSlot
    @Test
    void should403WhenInviteGuestsToDayEventSlotIsCalledWithInvalidCsrf() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s/invite"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // updateDayEventSlot
    @Test
    void should204WhenUpdateDayEventSlotIsSuccessful() throws Exception {
        DayEventSlotRequest eventSlotRequest = createDayEventSlotRequest();
        UUID slotId = UUID.randomUUID();

        doNothing().when(this.dayEventSlotService).updateEventSlot(1L, slotId, eventSlotRequest);

        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.dayEventSlotService, times(1)).updateEventSlot(1L, slotId, eventSlotRequest);
    }

    // updateDayEventSlot
    @Test
    void should404WhenDayEventSlotDoesNotExistForUpdateDayEventSlot() throws Exception {
        DayEventSlotRequest eventSlotRequest = createDayEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event slot not found with id: %s",
                    "path": "%s/%s"
                }
                """, slotId, DAY_EVENT_SLOT_PATH, slotId);

        doThrow(new ResourceNotFoundException("Day event slot not found with id: " + slotId)).when(this.dayEventSlotService).updateEventSlot(1L, slotId, eventSlotRequest);

        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );

        verify(this.dayEventSlotService, times(1)).updateEventSlot(1L, slotId, eventSlotRequest);
    }

    // updateDayEventSlot
    @Test
    void should409WhenOrganizerEmailIsInGuestListForUpdateDayEventSlot() throws Exception {
        DayEventSlotRequest eventSlotRequest = createDayEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 409,
                    "type": "CONFLICT",
                    "message": "Organizer of the event can't be added as guest",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);

        doThrow(new ConflictException("Organizer of the event can't be added as guest")).when(this.dayEventSlotService).updateEventSlot(1L, slotId, eventSlotRequest);

        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody, false)
                );

        verify(this.dayEventSlotService, times(1)).updateEventSlot(1L, slotId, eventSlotRequest);
    }

    // updateDayEventSlot
    @Test
    void should401WhenUpdateDayEventSlotIsCalledByUnauthenticatedUser() throws Exception {
        DayEventSlotRequest eventSlotRequest = createDayEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // updateDayEventSlot
    @Test
    void should403WhenUpdateDayEventSlotIsCalledWithNoCsrf() throws Exception {
        DayEventSlotRequest eventSlotRequest = createDayEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // updateDayEventSlot
    @Test
    void should403WhenUpdateDayEventSlotIsCalledWithInvalidCsrf() throws Exception {
        DayEventSlotRequest eventSlotRequest = createDayEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);


        this.mockMvc.perform(put(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // findDayEventSlotById
    @Test
    void should200WithDayEventSlotDTOForFindDayEventSlotById() throws Exception {
        UUID slotId = UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d");
        DayEventSlotPublicProjection eventSlot = DayEventSlotPublicProjection.builder()
                .id(slotId)
                .title("Title")
                .startDate(LocalDate.parse("2024-09-29"))
                .endDate(LocalDate.parse("2024-12-20"))
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .eventId(UUID.fromString("9c6f34b8-4128-42ec-beb1-99c35af8d7fa"))
                .build();

        when(this.dayEventSlotService.findEventSlotById(1L, slotId)).thenReturn(eventSlot);

        this.mockMvc.perform(get(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(eventSlot))
                );
    }

    // findDayEventSlotById
    @Test
    void should404WhenDayEventSlotDoesNotExistForFindDayEventSlotById() throws Exception {
        UUID slotId = UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d");
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event slot not found with id: %s",
                    "path": "%s/%s"
                }
                """, slotId, DAY_EVENT_SLOT_PATH, slotId);

        when(this.dayEventSlotService.findEventSlotById(1L, slotId)).thenThrow(new ResourceNotFoundException("Day event slot not found with id: " + slotId));

        this.mockMvc.perform(get(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // findDayEventSlotById
    @Test
    void should401WhenFindDayEventSlotByIdIsCalledByUnauthenticatedUser() throws Exception {
        UUID slotId = UUID.fromString("eede21d1-c2f1-4dc8-9913-a173c491f07d");
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(get(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // deleteDayEventSlotById
    @Test
    void should204WhenDeleteDayEventSlotByIdIsSuccessful() throws Exception {
        UUID slotId = UUID.randomUUID();

        doNothing().when(this.dayEventSlotService).deleteEventSlotById(slotId, 1L);

        this.mockMvc.perform(delete(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(
                        status().isNoContent()
                );

        verify(this.dayEventSlotService, times(1)).deleteEventSlotById(slotId, 1L);
    }

    // deleteDayEventSlotById
    @Test
    void should404WhenDayEventSlotDoesNotExistForDeleteDayEventSlotById() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Day event slot not found with id: %s",
                    "path": "%s/%s"
                }
                """, slotId, DAY_EVENT_SLOT_PATH, slotId);

        doThrow(new ResourceNotFoundException("Day event slot not found with id: " + slotId)).when(this.dayEventSlotService).deleteEventSlotById(slotId, 1L);

        this.mockMvc.perform(delete(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );

        verify(this.dayEventSlotService, times(1)).deleteEventSlotById(slotId, 1L);
    }

    // deleteDayEventSlotById
    @Test
    void should401WhenDeleteDayEventSlotIsCalledByUnauthenticatedUser() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(delete(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // deleteDayEventSlotById
    @Test
    void should403WhenDeleteDayEventSlotIsCalledWithNoCsrf() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(delete(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // deleteDayEventSlotById
    @Test
    void should403WhenDeleteDayEventSlotIsCalledWithInvalidCsrf() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, DAY_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(delete(DAY_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.dayEventSlotService);
    }

    // inviteGuestToTimeEventSlot
    @Test
    void should204WhenInviteGuestsToTimeEventSlotIsSuccessfulForTimeEventSlot() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();

        doNothing().when(this.timeEventSlotService).inviteGuests(1L, slotId, request);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.timeEventSlotService, times(1)).inviteGuests(1L, slotId, request);
    }

    // inviteGuestToTimeEventSlot
    @Test
    void should404WhenTimeEventSlotDoesNotExistForInviteGuestsToTimeEventSlot() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event slot not found with id: %s",
                    "path": "%s/%s/invite"
                }
                """, slotId, TIME_EVENT_SLOT_PATH, slotId);

        doThrow(new ResourceNotFoundException("Time event slot not found with id: " + slotId)).when(this.timeEventSlotService).inviteGuests(1L, slotId, request);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );

        verify(this.timeEventSlotService, times(1)).inviteGuests(1L, slotId, request);
    }

    // inviteGuestToTimeEventSlot
    @Test
    void should409WhenOrganizerEmailIsInGuestListForInviteGuestsToTimeEventSlot() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 409,
                    "type": "CONFLICT",
                    "message": "Organizer of the event can't be added as guest",
                    "path": "%s/%s/invite"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        doThrow(new ConflictException("Organizer of the event can't be added as guest")).when(this.timeEventSlotService).inviteGuests(1L, slotId, request);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody, false)
                );

        verify(this.timeEventSlotService, times(1)).inviteGuests(1L, slotId, request);
    }

    // inviteGuestToTimeEventSlot
    @Test
    void should401WhenInviteGuestsToTimeEventSlotIsCalledByUnauthenticatedUser() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s/invite"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // inviteGuestToTimeEventSlot
    @Test
    void should403WhenInviteGuestsToTimeEventSlotIsCalledWithNoCsrf() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s/invite"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // inviteGuestToTimeEventSlot
    @Test
    void should403WhenInviteGuestsToTimeEventSlotIsCalledWithInvalidCsrf() throws Exception {
        InviteGuestsRequest request = new InviteGuestsRequest(Set.of(FAKER.internet().emailAddress()));
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s/invite"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}/invite", slotId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // updateTimeEventSlot
    @Test
    void should204WhenUpdateTimeEventSlotIsSuccessful() throws Exception {
        TimeEventSlotRequest eventSlotRequest = createTimeEventSlotRequest();
        UUID slotId = UUID.randomUUID();

        doNothing().when(this.timeEventSlotService).updateEventSlot(1L, slotId, eventSlotRequest);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.timeEventSlotService, times(1)).updateEventSlot(1L, slotId, eventSlotRequest);
    }

    // updateTimeEventSlot
    @Test
    void should404WhenTimeEventSlotDoesNotExistForUpdateTimeEventSlot() throws Exception {
        TimeEventSlotRequest eventSlotRequest = createTimeEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event slot not found with id: %s",
                    "path": "%s/%s"
                }
                """, slotId, TIME_EVENT_SLOT_PATH, slotId);

        doThrow(new ResourceNotFoundException("Time event slot not found with id: " + slotId)).when(this.timeEventSlotService).updateEventSlot(1L, slotId, eventSlotRequest);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );

        verify(this.timeEventSlotService, times(1)).updateEventSlot(1L, slotId, eventSlotRequest);
    }

    // updateTimeEventSlot
    @Test
    void should409WhenOrganizerEmailIsInGuestListForUpdateTimeEventSlot() throws Exception {
        TimeEventSlotRequest eventSlotRequest = createTimeEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 409,
                    "type": "CONFLICT",
                    "message": "Organizer of the event can't be added as guest",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        doThrow(new ConflictException("Organizer of the event can't be added as guest")).when(this.timeEventSlotService).updateEventSlot(1L, slotId, eventSlotRequest);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest))
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isConflict(),
                        content().json(responseBody, false)
                );

        verify(this.timeEventSlotService, times(1)).updateEventSlot(1L, slotId, eventSlotRequest);
    }

    // updateTimeEventSlot
    @Test
    void should401WhenUpdateTimeEventSlotIsCalledByUnauthenticatedUser() throws Exception {
        TimeEventSlotRequest eventSlotRequest = createTimeEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // updateTimeEventSlot
    @Test
    void should403WhenUpdateTimeEventSlotIsCalledWithNoCsrf() throws Exception {
        TimeEventSlotRequest eventSlotRequest = createTimeEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // updateTimeEventSlot
    @Test
    void should403WhenUpdateTimeEventSlotIsCalledWithInvalidCsrf() throws Exception {
        TimeEventSlotRequest eventSlotRequest = createTimeEventSlotRequest();
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(put(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(eventSlotRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // findTimeEventSlotById
    @Test
    void should200WithTimeEventSlotDTOForFindTimeEventSlotById() throws Exception {
        UUID slotId = UUID.fromString("e431687e-7251-4ac6-b797-c107064af135");
        TimeEventSlotPublicProjection eventSlot = TimeEventSlotPublicProjection.builder()
                .id(slotId)
                .title("Event title")
                .location("Location")
                .description("Description")
                .organizer("ellyn.roberts")
                .guestEmails(Set.of())
                .startTime(LocalDateTime.parse("2024-10-11T10:00:00"))
                .endTime(LocalDateTime.parse("2024-10-15T15:00:00"))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .eventId(UUID.fromString("6b9b32f2-3c2a-4420-9d52-781c09f320ce"))
                .build();

        when(this.timeEventSlotService.findEventSlotById(1L, slotId)).thenReturn(eventSlot);

        this.mockMvc.perform(get(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(eventSlot))
                );
    }

    // findTimeEventSlotById
    @Test
    void should404WhenTimeEventSlotDoesNotExistForFindTimeEventSlotById() throws Exception {
        UUID slotId = UUID.fromString("e431687e-7251-4ac6-b797-c107064af135");
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event slot not found with id: %s",
                    "path": "%s/%s"
                }
                """, slotId, TIME_EVENT_SLOT_PATH, slotId);

        when(this.timeEventSlotService.findEventSlotById(1L, slotId)).thenThrow(new ResourceNotFoundException("Time event slot not found with id: " + slotId));

        this.mockMvc.perform(get(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );
    }

    // findTimeEventSlotById
    @Test
    void should401WhenFindTimeEventSlotByIdIsCalledByUnauthenticatedUser() throws Exception {
        UUID slotId = UUID.fromString("e431687e-7251-4ac6-b797-c107064af135");
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(get(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // deleteTimeEventSlotById
    @Test
    void should204WhenDeleteTimeEventSlotByIdIsSuccessful() throws Exception {
        UUID slotId = UUID.randomUUID();

        doNothing().when(this.timeEventSlotService).deleteEventSlotById(slotId, 1L);

        this.mockMvc.perform(delete(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.timeEventSlotService, times(1)).deleteEventSlotById(slotId, 1L);
    }

    // deleteTimeEventSlotById
    @Test
    void should404WhenTimeEventSlotDoesNotExistForDeleteTimeEventSlotById() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 404,
                    "type": "NOT_FOUND",
                    "message": "Time event slot not found with id: %s",
                    "path": "%s/%s"
                }
                """, slotId, TIME_EVENT_SLOT_PATH, slotId);

        doThrow(new ResourceNotFoundException("Time event slot not found with id: " + slotId)).when(this.timeEventSlotService).deleteEventSlotById(slotId, 1L);

        this.mockMvc.perform(delete(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isNotFound(),
                        content().json(responseBody, false)
                );

        verify(this.timeEventSlotService, times(1)).deleteEventSlotById(slotId, 1L);
    }

    // deleteTimeEventSlotById
    @Test
    void should401WhenDeleteTimeEventSlotIsCalledByUnauthenticatedUser() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(delete(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // deleteTimeEventSlotById
    @Test
    void should403WhenDeleteTimeEventSlotIsCalledWithNoCsrf() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(delete(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    // deleteTimeEventSlotById
    @Test
    void should403WhenDeleteTimeEventSlotIsCalledWithInvalidCsrf() throws Exception {
        UUID slotId = UUID.randomUUID();
        String responseBody = String.format("""
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "%s/%s"
                }
                """, TIME_EVENT_SLOT_PATH, slotId);

        this.mockMvc.perform(delete(TIME_EVENT_SLOT_PATH + "/{slotId}", slotId).with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthTestUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.timeEventSlotService);
    }

    private DayEventSlotRequest createDayEventSlotRequest() {
        return DayEventSlotRequest.builder()
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .title("Title")
                .location("New location")
                .guestEmails(Set.of("waltraud.roberts@gmail.com"))
                .build();
    }

    private TimeEventSlotRequest createTimeEventSlotRequest() {
        return TimeEventSlotRequest.builder()
                // We can't just say LocalDateTime.now(ZoneId.of("Europe/London")). Some time passes before creating
                // and validating and our date time now is in the past
                .startTime(LocalDateTime.now(ZoneId.of("Europe/London")).plusMinutes(1))
                .startTimeZoneId(ZoneId.of("Europe/London"))
                .endTime(LocalDateTime.now(ZoneId.of("Europe/London")).plusMinutes(30))
                .endTimeZoneId(ZoneId.of("Europe/London"))
                .title("Title")
                .location("New location")
                .guestEmails(Set.of("waltraud.roberts@gmail.com"))
                .build();
    }
}
