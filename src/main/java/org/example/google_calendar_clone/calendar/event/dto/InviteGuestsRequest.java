package org.example.google_calendar_clone.calendar.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record InviteGuestsRequest(@NotNull(message = "Provide at least one guest email address")
                                  @Size(min = 1, message = "Provide at least one guest email email address")
                                  Set<String> guestEmails) {
}
