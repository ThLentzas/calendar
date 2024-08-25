package org.example.google_calendar_clone.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import org.example.google_calendar_clone.entity.key.UserContactId;

@Entity
@Table(name = "user_contacts")
@Setter
@Getter
public class UserContact {
    @EmbeddedId
    private UserContactId id;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId1")
    @JoinColumn(name = "user_id_1")
    private User user1;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId2")
    @JoinColumn(name = "user_id_2")
    private User user2;
}
