package org.example.calendar.event.slot;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Size;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public abstract class AbstractEventSlotRequest {
    // null is considered valid for @Size()
    @Size(max = 50, message = "Title must not exceed 50 characters")
    protected String title;
    @Size(max = 50, message = "Location must not exceed 50 characters")
    protected String location;
    protected String description;
    protected Set<String> guestEmails;
}
