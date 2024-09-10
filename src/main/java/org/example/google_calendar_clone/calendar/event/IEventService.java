package org.example.google_calendar_clone.calendar.event;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/*
    If we don't use a Generic, we would have 2 implementations for the same interface, the DayEventService and the
    TimeEventService, so we would have to use @Qualifier() to specify which bean we need every time.
 */
public interface IEventService <T extends AbstractEventRequest> {
    // The organizer of the event is the current authenticated user
    @Transactional
    UUID create(Jwt jwt, T eventRequest);

    @Transactional
    void deleteById(Jwt jwt, UUID id);
}
