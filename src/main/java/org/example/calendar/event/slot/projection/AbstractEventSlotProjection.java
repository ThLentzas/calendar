package org.example.calendar.event.slot.projection;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractEventSlotProjection {
    @EqualsAndHashCode.Include
    protected UUID id;
    protected String title;
    protected String location;
    protected String description;
    protected Set<String> guestEmails;
}
