package org.example.google_calendar_clone.calendar.event;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.example.google_calendar_clone.calendar.event.day.DayEventService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.dto.validator.OnCreate;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {
    private final DayEventService dayEventService;

    // toDo: indexing
    @PostMapping("/day-events")
    ResponseEntity<Void> createDayEvent(@AuthenticationPrincipal Jwt jwt,
                                        @Validated(OnCreate.class) @RequestBody DayEventRequest dayEventRequest) {
        DayEvent dayEvent = this.dayEventService.create(jwt, dayEventRequest);
        /*
            https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-uri-building.html
            https://docs.spring.io/spring-framework/docs/4.1.2.RELEASE_to_4.1.3.RELEASE/Spring%20Framework%204.1.3.RELEASE/org/springframework/http/ResponseEntity.html
         */
        URI location = UriComponentsBuilder.fromUriString("/api/v1/events/day-events/{eventId}").build(dayEvent.getId());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(location);

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
    }

    @GetMapping("/day-events/{eventId}")
    ResponseEntity<List<DayEventSlotDTO>> findDayEventSlotsByEventId(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable("eventId") UUID eventId) {
        List<DayEventSlotDTO> dayEventSlots = this.dayEventService.findEventSlotsByEventId(jwt, eventId);

        return new ResponseEntity<>(dayEventSlots, HttpStatus.OK);
    }

    /*
        The offset refers to the difference in hours and minutes between a specific time zone and Coordinated Universal
        Time (UTC), which is often referred to as GMT (Greenwich Mean Time). When we see something like
        (GMT+03:00) Eastern European Time - Helsinki," it means that the time in that region is 3 hours ahead of (GMT)
     */
    @PostMapping("/time-events")
    ResponseEntity<Void> createTimeEvent(@AuthenticationPrincipal Jwt jwt,
                                         @Validated(OnCreate.class) @RequestBody TimeEventRequest timeEventRequest) {

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
