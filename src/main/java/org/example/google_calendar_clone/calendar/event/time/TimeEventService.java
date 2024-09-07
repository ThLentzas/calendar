package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.TimeEventSlotService;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeEventService implements IEventService<TimeEventRequest, TimeEvent> {
    private final UserRepository userRepository;
    private final TimeEventSlotService timeEventSlotService;
    private final TimeEventRepository timeEventRepository;

    @Override
    public TimeEvent create(Jwt jwt, TimeEventRequest eventRequest) {
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        TimeEvent timeEvent = TimeEvent.builder()
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .startTimeZoneId(eventRequest.getStartTimeZoneId())
                .endTimeZoneId(eventRequest.getEndTimeZoneId())
                .repetitionFrequency(eventRequest.getRepetitionFrequency())
                .repetitionStep(eventRequest.getRepetitionStep())
                .monthlyRepetitionType(eventRequest.getMonthlyRepetitionType())
                .repetitionDuration(eventRequest.getRepetitionDuration())
                .repetitionEndDate(eventRequest.getRepetitionEndDate())
                .repetitionCount(eventRequest.getRepetitionCount())
                .user(user)
                .build();
        /*
            For events that are set to be repeated for FOREVER we choose an arbitrary number like 100 years
            and set the repetition End Date to plus 100 years. We treat the event then as UNTIL_DATE but
            now the repetitionEndDate will be 100 years from now. This is 1 way to approach the FOREVER
            case
        */
        if (timeEvent.getRepetitionFrequency() != RepetitionFrequency.NEVER
                && timeEvent.getRepetitionDuration() == RepetitionDuration.FOREVER) {
            timeEvent.setRepetitionEndDate(LocalDate.from(timeEvent.getStartTime().plusYears(100)));
        }

        this.timeEventRepository.save(timeEvent);
        this.timeEventSlotService.create(eventRequest, timeEvent);

        return timeEvent;
    }
}
