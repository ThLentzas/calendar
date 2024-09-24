package org.example.calendar.entity.key;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode(of = {"senderId", "receiverId"})
public class ContactRequestId implements Serializable {
    private Long senderId;
    private Long receiverId;

    public ContactRequestId() {
    }

    public ContactRequestId(Long senderId, Long receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
    }
}
