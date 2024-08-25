package org.example.google_calendar_clone.user.contact.dto;

import jakarta.validation.constraints.NotNull;

import org.example.google_calendar_clone.user.contact.ContactRequestAction;

public record UpdateContactRequest(@NotNull(message = "You must provide the id of the sender")Long senderId,
                                   @NotNull(message = "You must provide an action")ContactRequestAction action) {
}
