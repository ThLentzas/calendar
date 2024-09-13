package org.example.google_calendar_clone.calendar.event;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.example.google_calendar_clone.calendar.event.day.slot.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.TimeEventService;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.slot.EventSlotComparator;
import org.example.google_calendar_clone.calendar.event.slot.EventSlotDTO;
import org.example.google_calendar_clone.validation.OnCreate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.net.URI;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

/*
    Day events:
        For repeating day events we need to handle the edge case where an event is to be repeated on the last the day of
        the month. This case is fully explained on the test of the DayEventSlotServiceTest class
            @Test
            void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayUntilACertainDate()

        We also need to handle an event that is repeated on 29 February during a leap year and, it is to be repeated
        annually on the same day. This case and the above case also explained to DateUtils.adjustDateForMonth() and
        in the tests of the DayEventSlotServiceTest class
            @Test
            void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayForNRepetitions()
            @Test
            void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNYearsAtUntilACertainDate()

    Time events:
        For time events we have to consider different timezones and their offset from GMT/UTC and Daylight saving(DST).
        It is a practice where clocks are set forward by one hour during spring to extend evening daylight and set back
        again by one hour during the fall to revert to standard time. Clocks are moved forward by one hour, typically
        in the spring, if the local time is 2:00 AM, it is changed to 3:00 AM. This is known as "springing forward."
        Clocks are moved back by one hour in the fall, if the local time is 2:00 AM, it is changed to 1:00 AM.
        This is known as "falling back."

        DST Gap:
            The DST gap occurs when clocks are moved forward, typically by one hour, resulting in a period of time that
            does not exist on that day.
            During the start of DST, typically in the spring. (second Sunday in March). At 2:00 AM, clocks are moved
            forward one hour to 3:00 AM. The time between 2:00 AM and 2:59:59 AM does not exist on that day. This is
            known as the DST gap. Any events scheduled to occur during this gap (e.g., 2:30 AM) are skipped
            because that time is skipped
        DST Overlap:
            The DST overlap occurs when clocks are moved backward, typically by one hour, creating a situation where a
            period of time occurs twice. During the end of DST, typically in the fall(first Sunday in November).At
            2:00 AM, clocks are moved backward one hour to 1:00 AM. The time between 1:00 AM and 1:59:59 AM occurs
            twice on that day. This is known as the DST overlap. Events scheduled during this overlap can occur at
            two distinct moments in time:
                First occurrence: Before the clocks are set back (DST, UTC-4).
                Second occurrence: After the clocks are set back (Standard Time, UTC-5).

        Not all regions observe DST. For the DST gap or overlap to occur, the region must be one that adjusts its
        clocks for DST. For instance, America/New_York supports DST, while Asia/Kolkata does not.

        Java's time api handles these 2 cases for us:
        The documentation of the method: public static ZonedDateTime ofLocal(LocalDateTime localDateTime, ZoneId zone,
        ZoneOffset preferredOffset) of the ZonedDateTime.java class explains that if the date falls on a DST gaps it is
        moved 1 hour forward.
            "In the case of a gap, where clocks jump forward, there is no valid offset. Instead, the local date-time is
            adjusted to be later by the length of the gap. For a typical one hour daylight savings change, the local
            date-time will be moved one hour later into the offset typically corresponding to "summer"".
            If we have 2024-03-10T02:30 America/New_York which is a DST gap, the corresponding UTC time is adjusted to +1
            hour 2024-03-10T03:30-04:00[America/New_York]. It goes to 03:30 with an offset of -4

        The same method handles the DST overlap as well. If we have 2024-11-03T01:30 America/New_York which is a DST
        overlap according to the method's documentation: "In most cases, there is only one valid offset for a local
        date-time. In the case of an overlap, where clocks are set back, there are two valid offsets. If the preferred
        offset is one of the valid offsets then it is used. Otherwise, the earlier valid offset is used, typically
        corresponding to "summer"". The zoned date time will be 2024-11-03T01:30-04:00[America/New_York] which has as
        offset -04:00 which corresponds to the offset before the DST ends. During DST, offset is UTC - 4 and when DST
        ends it is UTC - 5. There are also methods like withEarlierOffsetAtOverlap() and withLaterOffsetAtOverlap()
        if we want to be more specific in those scenarios.

        In the TimeEventSlotServiceTest class there are test for both cases:
            @Test
            void shouldCreateTimeEventSlotForNonRepeatingEventDuringDSTGap()
            @Test
            void shouldCreateTimeEventSlotForNonRepeatingEventDuringDSTOverlap()

        UTC + 3 and UTC - 2 are offsets from UTC. The number represents how many hours we need add to or subtract from
        UTC to get the local time. UTC + 3 means that the local time is 3 hours ahead of UTC. UTC - 2 means that the
        local time is 2 hours behind UTC. A timezone like Eastern Standard Time (EST) has UTC - 5 when DST is off and
        UTC - 4 when DST is on. Java's time api will handle UTC offset adjustments if the DST is active for the given
        date time on the provided timezone. Examples at TimeEventSlotServiceTest class:
             @Test
             void shouldCreateDayEventSlotsWhenEventIsRepeatingEveryNDaysForNRepetitions()
             @Test
             void shouldCreateTimeEventSlotsWhenEventIsRepeatingEveryNMonthsAtTheSameDayForNRepetitions()
        How we handle time events?
            The user provides the start/end time and their respective timezones. Using Java's time api and the
            ZonedDateTime class, we convert the times provided by their user to UTC, and we can do that because we will
            know the offset from their respective timezones. When we want to show a time event to the user we convert
            the UTC time back to their local time according to their timezone.

    An alternative:
        @RestController
        @RequestMapping("/api/v1/events")
        class EventController {
            private final IEventService<DayEventRequest> dayEventService;
            private final IEventService<TimeEventRequest> timeEventService;

            public EventController(DayEventService dayEventService, TimeEventService timeEventService) {
                this.dayEventService = dayEventService;
                this.timeEventService = timeEventService;
            }
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {
    private final DayEventService dayEventService;
    private final TimeEventService timeEventService;

    // toDo: indexing
    @PostMapping("/day-events")
    ResponseEntity<Void> createDayEvent(@AuthenticationPrincipal Jwt jwt,
                                        @Validated(OnCreate.class) @RequestBody DayEventRequest dayEventRequest) {
        UUID eventId = this.dayEventService.create(jwt, dayEventRequest);
        /*
            https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-uri-building.html
            https://docs.spring.io/spring-framework/docs/4.1.2.RELEASE_to_4.1.3.RELEASE/Spring%20Framework%204.1.3.RELEASE/org/springframework/http/ResponseEntity.html
         */
        URI location = UriComponentsBuilder.fromUriString("/api/v1/events/day-events/{eventId}").build(eventId);
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

    // DayEventSlots for the given DayEvent are deleted by ON DELETE CASCADE
    @DeleteMapping("/day-events/{eventId}")
    ResponseEntity<Void> deleteDayEventById(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable("eventId") UUID eventId) {
        this.dayEventService.deleteById(jwt, eventId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/time-events")
    ResponseEntity<Void> createTimeEvent(@AuthenticationPrincipal Jwt jwt,
                                         @Validated(OnCreate.class) @RequestBody TimeEventRequest timeEventRequest) {
        UUID eventId = this.timeEventService.create(jwt, timeEventRequest);
        URI location = UriComponentsBuilder.fromUriString("/api/v1/events/time-events/{eventId}").build(eventId);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(location);

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
    }

    @GetMapping("/time-events/{eventId}")
    ResponseEntity<List<TimeEventSlotDTO>> findTimeEventSlotsByEventId(@AuthenticationPrincipal Jwt jwt,
                                                                       @PathVariable("eventId") UUID eventId) {
        List<TimeEventSlotDTO> timeEventSlots = this.timeEventService.findEventSlotsByEventId(jwt, eventId);

        return new ResponseEntity<>(timeEventSlots, HttpStatus.OK);
    }

    // TimeEventSlots for the given TimeEvent are deleted by ON DELETE CASCADE
    @DeleteMapping("/time-events/{eventId}")
    ResponseEntity<Void> deleteTimeEventById(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable("eventId") UUID eventId) {
        this.timeEventService.deleteById(jwt, eventId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        We return all the events(DayEvents and TimeEvents) that are within a range of dates. The user is either the
        organizer of the event or an invited guest.

        If startDate > endDate we return an empty list
     */
    @GetMapping
    ResponseEntity<List<EventSlotDTO>> findEventsByUserInDateRange(@RequestParam(value = "start")
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                   LocalDate startDate,
                                                                   @RequestParam(value = "end")
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                   LocalDate endDate,
                                                                   @AuthenticationPrincipal Jwt jwt) {
        List<DayEventSlotDTO> dayEventSlots = this.dayEventService.findEventSlotsByUserInDateRange(
                jwt,
                startDate,
                endDate
        );
        List<EventSlotDTO> eventSlots = new ArrayList<>(dayEventSlots);
        eventSlots.addAll(this.timeEventService.findEventSlotsByUserInDateRange(
                jwt,
                // converts a LocalDate into a LocalDateTime adding time of the midnight as 00:00:00
                startDate.atStartOfDay(),
                endDate.atStartOfDay())
        );
        /*
            Both the DayEventSlots and TimeEventSlots are sorted but when we add them in 1 list, we need to make sure
            that they are also sorted based on their starting date. We need a comparator so that we can compare the
            starting date of DayEventSlot with the starting dateTime of the TimeEventSlot. If we have 2 DayEventSlots
            we compare their starting date, if we have 2 TimeEventSlots we compare their starting time.
         */
        eventSlots.sort(new EventSlotComparator());

        return new ResponseEntity<>(eventSlots, HttpStatus.OK);
    }
}
