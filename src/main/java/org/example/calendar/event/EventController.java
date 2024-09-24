package org.example.calendar.event;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.example.calendar.event.day.DayEventService;
import org.example.calendar.event.day.dto.DayEventRequest;
import org.example.calendar.event.time.dto.TimeEventRequest;
import org.example.calendar.event.time.TimeEventService;
import org.example.calendar.event.slot.EventSlotComparator;
import org.example.calendar.event.slot.time.dto.TimeEventSlotDTO;
import org.example.calendar.event.slot.AbstractEventSlotDTO;
import org.example.calendar.event.slot.day.dto.DayEventSlotDTO;
import org.example.calendar.event.groups.OnCreate;
import org.example.calendar.event.groups.OnUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.net.URI;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

/*
    DB schema: https://kb.databasedesignbook.com/posts/google-calendar/

    Day events:
        For recurring day events we need to handle the edge case where an event is to occur on the last the day of
        the month. This case is fully explained on the test of the DayEventSlotServiceTest class
            @Test
            void shouldCreateDayEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameDayUntilACertainDate()

        We also need to handle an event that occurs on 29 February during a leap year and, it is to occur
        annually on the same day. This case and the above case also explained to DateUtils.adjustDateForMonth() and
        in the tests of the DayEventSlotServiceTest class
            @Test
            void shouldCreateDayEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameDayForNOccurrences()
            @Test
            void shouldCreateDayEventSlotsWhenEventIsRecurringEveryNYearsAtUntilACertainDate()

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
            void shouldCreateTimeEventSlotForNonRecurringEventDuringDSTGap()
            @Test
            void shouldCreateTimeEventSlotForNonRecurringEventDuringDSTOverlap()

        UTC + 3 and UTC - 2 are offsets from UTC. The number represents how many hours we need add to or subtract from
        UTC to get the local time. UTC + 3 means that the local time is 3 hours ahead of UTC. UTC - 2 means that the
        local time is 2 hours behind UTC. A timezone like Eastern Standard Time (EST) has UTC - 5 when DST is off and
        UTC - 4 when DST is on. Java's time api will handle UTC offset adjustments if the DST is active for the given
        date time on the provided timezone. Examples at TimeEventSlotServiceTest class:
             @Test
             void shouldCreateDayEventSlotsWhenEventIsRecurringEveryNDaysForNROccurrences()
             @Test
             void shouldCreateTimeEventSlotsWhenEventIsRecurringEveryNMonthsAtTheSameDayForNOccurrences()
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

      I also tried this:
        Generics are invariant. EventSchedule(List<AbstractEventSlotDTO> eventSlots) would not work. When we try to pass
        a list of either DayEventSlotDTO or TimeEventSlotDTO, because those do extend AbstractEvenSlotDTO, but their
        respective List<> won't due to generics being invariant
        public record EventSchedule(List<? extends AbstractEventSlotDTO> eventSlots) {
        } Had problems with Serialization and Jackson when i was using the EventSchedule class.
        <List<AbstractEventSlotDTO>> findEventsByUserInDateRange For this method Jackson had no issues to serialize the
        objects.
        Jackson was having trouble deserializing the polymorphic type (list of AbstractEventSlotDTO inside
        EventSchedule) because the list was a field within another object (EventSchedule), and Jackson couldn't infer
        the concrete type from an abstract class. The solution was to use @JsonTypeInfo and @JsonSubTypes

     IMPORTANT!!! We don't pass the Jwt in the service to extract the userId. Service layer should not know anything
     about jwt/auth mechanism.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {
    private final DayEventService dayEventService;
    private final TimeEventService timeEventService;

    @PostMapping("/day-events")
    ResponseEntity<Void> createDayEvent(@AuthenticationPrincipal Jwt jwt,
                                        @Validated(OnCreate.class) @RequestBody DayEventRequest eventRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        UUID eventId = this.dayEventService.create(userId, eventRequest);
        /*
            https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-uri-building.html
            https://docs.spring.io/spring-framework/docs/4.1.2.RELEASE_to_4.1.3.RELEASE/Spring%20Framework%204.1.3.RELEASE/org/springframework/http/ResponseEntity.html
         */
        URI location = UriComponentsBuilder.fromUriString("/api/v1/events/day-events/{eventId}").build(eventId);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(location);

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
    }

    /*
        For the given day event, updates its day event slots. If new frequency details are provided, we create new
        day event slots based on the new details and delete the old ones. If properties like title, location, guests
        etc. are also provided, we update those properties for every event slot. For the given event, the frequency
        details are also updated.
     */
    @PutMapping("/day-events/{eventId}")
    ResponseEntity<Void> updateDayEvent(@PathVariable("eventId") UUID eventId,
                                        @AuthenticationPrincipal Jwt jwt,
                                        @Validated(OnUpdate.class) @RequestBody DayEventRequest eventRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.dayEventService.update(userId, eventId, eventRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/day-events/{eventId}")
    ResponseEntity<List<DayEventSlotDTO>> findDayEventSlotsByEventId(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable("eventId") UUID eventId) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<DayEventSlotDTO> dayEventSlots = this.dayEventService.findEventSlotsByEventId(eventId, userId);

        return new ResponseEntity<>(dayEventSlots, HttpStatus.OK);
    }

    // DayEventSlots for the given DayEvent are deleted by ON DELETE CASCADE
    @DeleteMapping("/day-events/{eventId}")
    ResponseEntity<Void> deleteDayEventById(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable("eventId") UUID eventId) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.dayEventService.deleteEventById(eventId, userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/time-events")
    ResponseEntity<Void> createTimeEvent(@AuthenticationPrincipal Jwt jwt,
                                         @Validated(OnCreate.class) @RequestBody TimeEventRequest eventRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        UUID eventId = this.timeEventService.create(userId, eventRequest);
        URI location = UriComponentsBuilder.fromUriString("/api/v1/events/time-events/{eventId}").build(eventId);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setLocation(location);

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
    }

    /*
        For the given day event, updates its day event slots. If new frequency details are provided, we create new
        day event slots based on the new details and delete the old ones. If properties like title, location, guests
        etc. are also provided, we update those properties for every event slot. For the given event, the frequency
        details are also updated.
     */
    @PutMapping("/time-events/{eventId}")
    ResponseEntity<Void> updateTimeEvent(@PathVariable("eventId") UUID eventId,
                                         @AuthenticationPrincipal Jwt jwt,
                                         @Validated(OnUpdate.class) @RequestBody TimeEventRequest eventRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.timeEventService.update(userId, eventId, eventRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/time-events/{eventId}")
    ResponseEntity<List<TimeEventSlotDTO>> findTimeEventSlotsByEventId(@AuthenticationPrincipal Jwt jwt,
                                                                       @PathVariable("eventId") UUID eventId) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<TimeEventSlotDTO> timeEventSlots = this.timeEventService.findEventSlotsByEventId(eventId, userId);

        return new ResponseEntity<>(timeEventSlots, HttpStatus.OK);
    }

    // TimeEventSlots for the given TimeEvent are deleted by ON DELETE CASCADE
    @DeleteMapping("/time-events/{eventId}")
    ResponseEntity<Void> deleteTimeEventById(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable("eventId") UUID eventId) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.timeEventService.deleteEventById(eventId, userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /*
        We return all the events(DayEvents and TimeEvents) that are within a range of dates. The user is either the
        organizer of the event or an invited guest.

        If startDate > endDate we return an empty list
     */
    @GetMapping
    ResponseEntity<List<AbstractEventSlotDTO>> findEventsByUserInDateRange(@RequestParam(value = "start")
                                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                           LocalDate startDate,
                                                                           @RequestParam(value = "end")
                                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                           LocalDate endDate,
                                                                           @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<DayEventSlotDTO> dayEventSlots = this.dayEventService.findEventSlotsByUserInDateRange(userId, startDate, endDate);
        List<AbstractEventSlotDTO> eventSlots = new ArrayList<>(dayEventSlots);
        // converts a LocalDate into a LocalDateTime adding time of the midnight as 00:00:00
        eventSlots.addAll(this.timeEventService.findEventSlotsByUserInDateRange(userId, startDate.atStartOfDay(), endDate.atStartOfDay()));
        /*
            Both the DayEventSlots and TimeEventSlots are sorted but when we add them in 1 list, we need to make sure
            that they are also sorted based on their starting date. We need a comparator so that we can compare the
            starting date of DayEventSlot with the starting dateTime of the TimeEventSlot. If we have 2 DayEventSlots
            we compare their starting date, if we have 2 TimeEventSlots we compare their starting time.
         */
        eventSlots.sort(new EventSlotComparator());

        /*
            Jackson will serialize the list correctly. Even though the list is of type AbstractEventSlotDTO,
            the objects themselves are instances of DayEventSlotDTO or TimeEventSlotDTO. Jackson inspects each object
            to determine its actual class and serializes it accordingly. It is capable of serializing subclass-specific
            fields because it operates on the actual objects' runtime types, not just their declared reference types.
            This allows it to include all relevant fields in the JSON output (fields like startDate and startTime are
            not part of the AbstractEventSlotDTO. It serializes the object based on its actual type, not the reference
            type in the list)
         */
        return new ResponseEntity<>(eventSlots, HttpStatus.OK);
    }
}
