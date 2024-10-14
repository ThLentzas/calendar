package org.example.calendar.event.slot.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class EventSlotWithGuestsProjection implements GuestProjection {
    private UUID id;
    private Set<String> guestEmails;
}
