package org.example.google_calendar_clone.calendar.event.day.slot;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.google_calendar_clone.calendar.event.dto.InviteGuestRequest;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/day-event-slots")
@RequiredArgsConstructor
public class DayEventSlotController {
    private final DayEventSlotService dayEventSlotService;

    @PutMapping("/day-event-slot/{slotId}/invite")
    ResponseEntity<Void> inviteGuestsToDayEventSlot(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("slotId") UUID slotId,
                                                    @RequestBody InviteGuestRequest inviteGuestRequest) {
        this.dayEventSlotService.inviteGuests(jwt, slotId, inviteGuestRequest);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
