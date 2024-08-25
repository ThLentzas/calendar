package org.example.google_calendar_clone.user.contact.dto;

import jakarta.validation.constraints.NotNull;

public record CreateContactRequest(@NotNull(message = "You must provide the id of the receiver") Long receiverId) {
}
