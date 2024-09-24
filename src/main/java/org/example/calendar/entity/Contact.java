package org.example.calendar.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.example.calendar.entity.key.ContactId;

@Entity
@Table(name = "contacts")
@Setter
@Getter
@EqualsAndHashCode(of = "id")
public class Contact {
    @EmbeddedId
    private ContactId id;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId1")
    @JoinColumn(name = "user_id_1")
    private User user1;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId2")
    @JoinColumn(name = "user_id_2")
    private User user2;
}
