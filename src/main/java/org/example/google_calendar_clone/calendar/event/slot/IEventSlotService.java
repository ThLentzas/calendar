package org.example.google_calendar_clone.calendar.event.slot;

import java.util.List;
import java.util.UUID;

import org.example.google_calendar_clone.calendar.event.AbstractEvent;
import org.example.google_calendar_clone.calendar.event.AbstractEventRequest;
import org.springframework.transaction.annotation.Transactional;

/*
    Why we need both the event request and the event? Because the event slot needs information from the request that we
    do not persist for the event, but we need to event for the Hibernate relationship
 */
public interface IEventSlotService <T extends AbstractEventRequest, U extends AbstractEvent, K extends EventSlotDTO> {

    @Transactional
    void create(T eventRequest, U event);

    List<K> findEventSlotsByEventId(UUID eventId);
}
