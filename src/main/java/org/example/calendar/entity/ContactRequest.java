package org.example.calendar.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.example.calendar.user.contact.request.ContactRequestStatus;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"senderId", "receiverId"})
public class ContactRequest {
    private Long senderId;
    private Long receiverId;
    private ContactRequestStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
