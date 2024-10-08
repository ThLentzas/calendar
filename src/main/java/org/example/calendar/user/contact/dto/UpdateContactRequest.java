package org.example.calendar.user.contact.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.example.calendar.user.contact.request.ContactRequestAction;

public record UpdateContactRequest(@NotNull(message = "You must provide the id of the sender")
                                   @Positive Long senderId,
                                   @NotNull(message = "You must provide an action") ContactRequestAction action) {
}
