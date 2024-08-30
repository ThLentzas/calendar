package org.example.google_calendar_clone.calendar.event;

import org.springframework.transaction.annotation.Transactional;

/*
    Why we need both the event request and the event? Because the event slot needs information from the request that we
    do not persist for the event, but we need to event for the Hibernate relationship
 */
public interface IEventSlotService <T extends EventRequest, U extends AbstractEvent> {

    @Transactional
    void create(T eventRequest, U event);
}
