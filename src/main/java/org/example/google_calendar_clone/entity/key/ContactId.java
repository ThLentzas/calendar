package org.example.google_calendar_clone.entity.key;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode(of = {"userId1", "userId2"})
public class ContactId implements Serializable {
    private Long userId1;
    private Long userId2;

    public ContactId() {
    }

    public ContactId(Long userId1, Long userId2) {
        this.userId1 = userId1;
        this.userId2 = userId2;
    }
}
