package org.example.google_calendar_clone.user.contact.dto;

import org.example.google_calendar_clone.user.contact.ContactRequestStatus;
import org.example.google_calendar_clone.user.dto.UserProfile;

public record PendingContactRequest(UserProfile userProfile, ContactRequestStatus status) {
}
