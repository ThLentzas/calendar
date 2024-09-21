package org.example.google_calendar_clone.calendar.event.slot;

import lombok.RequiredArgsConstructor;

import org.example.google_calendar_clone.calendar.event.dto.InviteGuestsRequest;
import org.example.google_calendar_clone.calendar.event.slot.day.DayEventSlotService;
import org.example.google_calendar_clone.calendar.event.slot.day.dto.DayEventSlotDTO;
import org.example.google_calendar_clone.calendar.event.slot.time.TimeEventSlotService;
import org.example.google_calendar_clone.calendar.event.slot.time.dto.TimeEventSlotDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/event-slots")
@RequiredArgsConstructor
class EventSlotController {
    private final DayEventSlotService dayEventSlotService;
    private final TimeEventSlotService timeEventSlotService;

    @PutMapping("/day-event-slots/{slotId}/invite")
    ResponseEntity<Void> inviteGuestsToDayEventSlot(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("slotId") UUID slotId,
                                                    @RequestBody InviteGuestsRequest inviteGuestsRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.dayEventSlotService.inviteGuests(userId, slotId, inviteGuestsRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/day-event-slots/{slotId}")
    ResponseEntity<DayEventSlotDTO> findDayEventSlotById(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable("slotId") UUID slotId) {
        Long userId = Long.valueOf(jwt.getSubject());
        DayEventSlotDTO eventSlot = this.dayEventSlotService.findByUserAndSlotId(userId, slotId);

        return new ResponseEntity<>(eventSlot, HttpStatus.OK);
    }

    @PutMapping("/time-event-slots/{slotId}/invite")
    ResponseEntity<Void> inviteGuestsToTimeEventSlot(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable("slotId") UUID slotId,
                                                     @RequestBody InviteGuestsRequest inviteGuestsRequest) {
        Long userId = Long.valueOf(jwt.getSubject());
        this.timeEventSlotService.inviteGuests(userId, slotId, inviteGuestsRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/time-event-slots/{slotId}")
    ResponseEntity<TimeEventSlotDTO> findTimeEventSlotById(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable("slotId") UUID slotId) {
        Long userId = Long.valueOf(jwt.getSubject());
        TimeEventSlotDTO eventSlot = this.timeEventSlotService.findByUserAndSlotId(userId, slotId);

        return new ResponseEntity<>(eventSlot, HttpStatus.OK);
    }
}
