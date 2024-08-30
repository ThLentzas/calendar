package org.example.google_calendar_clone.calendar.event.day;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.day.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.day.slot.DayEventSlotService;
import org.example.google_calendar_clone.entity.DayEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEventService implements IEventService<DayEventRequest, DayEvent> {
    private final DayEventSlotService dayEventSlotService;
    private final DayEventRepository dayEventRepository;
    private final UserRepository userRepository;

    //toDo: add invite link in the dto
    @Override
    public DayEvent create(Jwt jwt, DayEventRequest dayEventRequest) {
        // The current authenticated user is the organizer of the event
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        DayEvent dayEvent = new DayEvent();
        dayEvent.setStartDate(dayEventRequest.getStartDate());
        dayEvent.setEndDate(dayEventRequest.getEndDate());
        dayEvent.setRepetitionFrequency(dayEventRequest.getRepetitionFrequency());
        dayEvent.setRepetitionStep(dayEventRequest.getRepetitionStep());
        dayEvent.setMonthlyRepetitionType(dayEventRequest.getMonthlyRepetitionType());
        dayEvent.setRepetitionDuration(dayEventRequest.getRepetitionDuration());
        dayEvent.setRepetitionEndDate(dayEventRequest.getRepetitionEndDate());
        dayEvent.setRepetitionCount(dayEventRequest.getRepetitionCount());
        dayEvent.setCurrentRepetition(dayEventRequest.getRepetitionCount() == null ? null : 0);
        dayEvent.setUser(user);

        this.dayEventRepository.save(dayEvent);
        this.dayEventSlotService.create(dayEventRequest, dayEvent);

        return dayEvent;
    }
}
