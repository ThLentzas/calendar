package org.example.google_calendar_clone.calendar.event;

import org.example.google_calendar_clone.calendar.event.slot.EventSlotDTO;
import org.springframework.transaction.annotation.Transactional;
import org.example.google_calendar_clone.calendar.event.dto.AbstractEventRequest;

import java.util.List;
import java.util.UUID;

/*
    If we don't use a Generic, we would have 2 implementations for the same interface, the DayEventService and the
    TimeEventService, so we would have to use @Qualifier() to specify which bean we need every time.
 */
public interface IEventService <T extends AbstractEventRequest, U extends EventSlotDTO> {
    @Transactional
    UUID create(Long userId, T eventRequest);

    @Transactional
    void deleteById(Long userId, UUID id);

    List<U> findEventSlotsByEventId(Long userId, UUID eventId);
}
