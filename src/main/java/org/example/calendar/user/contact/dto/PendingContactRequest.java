package org.example.calendar.user.contact.dto;

import org.example.calendar.user.contact.request.ContactRequestStatus;
import org.example.calendar.user.dto.UserProfile;

public record PendingContactRequest(UserProfile userProfile, ContactRequestStatus status) {
}
