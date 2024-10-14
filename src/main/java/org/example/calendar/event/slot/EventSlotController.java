package org.example.calendar.event.slot;

import jakarta.validation.Valid;
import org.example.calendar.event.slot.day.DayEventSlotService;
import org.example.calendar.event.slot.day.projection.DayEventSlotPublicProjection;
import org.example.calendar.event.slot.day.dto.DayEventSlotRequest;
import org.example.calendar.event.slot.time.TimeEventSlotService;
import org.example.calendar.event.slot.time.projection.TimeEventSlotPublicProjection;
import org.example.calendar.event.slot.time.dto.TimeEventSlotRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.example.calendar.event.dto.InviteGuestsRequest;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

/*
     IMPORTANT!!! We don't pass the Jwt in the service to extract the userId. Service layer should not know anything
     about jwt/auth mechanism.
 */
@RestController
@RequestMapping("/api/v1/event-slots")
@RequiredArgsConstructor
class EventSlotController {
    private final DayEventSlotService dayEventSlotService;
    private final TimeEventSlotService timeEventSlotService;

    @PutMapping("/day-event-slots/{slotId}/invite")
    ResponseEntity<Void> inviteGuestsToDayEventSlot(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("slotId") UUID slotId,
                                                    @Valid @RequestBody InviteGuestsRequest inviteGuestsRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.dayEventSlotService.inviteGuests(userId, slotId, inviteGuestsRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/day-event-slots/{slotId}")
    ResponseEntity<Void> updateDayEventSlot(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable("slotId") UUID slotId,
                                            @Validated @RequestBody DayEventSlotRequest eventSlotRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.dayEventSlotService.updateEventSlot(userId, slotId, eventSlotRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/day-event-slots/{slotId}")
    ResponseEntity<DayEventSlotPublicProjection> findDayEventSlotById(@AuthenticationPrincipal Jwt jwt,
                                                                      @PathVariable("slotId") UUID slotId) {
        Long userId = Long.valueOf(jwt.getSubject());
        DayEventSlotPublicProjection eventSlot = this.dayEventSlotService.findEventSlotById(userId, slotId);

        return new ResponseEntity<>(eventSlot, HttpStatus.OK);
    }

    @DeleteMapping("/day-event-slots/{slotId}")
    ResponseEntity<Void> deleteDayEventSlotById(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable("slotId") UUID slotId) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.dayEventSlotService.deleteEventSlotById(slotId, userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/time-event-slots/{slotId}/invite")
    ResponseEntity<Void> inviteGuestsToTimeEventSlot(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable("slotId") UUID slotId,
                                                     @Valid @RequestBody InviteGuestsRequest inviteGuestsRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.timeEventSlotService.inviteGuests(userId, slotId, inviteGuestsRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/time-event-slots/{slotId}")
    ResponseEntity<Void> updateTimeEventSlot(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable("slotId") UUID slotId,
                                             @Validated @RequestBody TimeEventSlotRequest eventSlotRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.timeEventSlotService.updateEventSlot(userId, slotId, eventSlotRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/time-event-slots/{slotId}")
    ResponseEntity<TimeEventSlotPublicProjection> findTimeEventSlotById(@AuthenticationPrincipal Jwt jwt,
                                                                        @PathVariable("slotId") UUID slotId) {
        Long userId = Long.valueOf(jwt.getSubject());
        TimeEventSlotPublicProjection eventSlot = this.timeEventSlotService.findEventSlotById(userId, slotId);

        return new ResponseEntity<>(eventSlot, HttpStatus.OK);
    }

    @DeleteMapping("/time-event-slots/{slotId}")
    ResponseEntity<Void> deleteTimeEventSlotById(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable("slotId") UUID slotId) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.timeEventSlotService.deleteEventSlotById(slotId, userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
