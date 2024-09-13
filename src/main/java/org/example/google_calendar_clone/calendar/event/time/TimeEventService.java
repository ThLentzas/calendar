package org.example.google_calendar_clone.calendar.event.time;

import org.example.google_calendar_clone.calendar.event.IEventService;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionDuration;
import org.example.google_calendar_clone.calendar.event.repetition.RepetitionFrequency;
import org.example.google_calendar_clone.calendar.event.time.dto.TimeEventRequest;
import org.example.google_calendar_clone.calendar.event.time.slot.TimeEventSlotService;
import org.example.google_calendar_clone.calendar.event.time.slot.dto.TimeEventSlotDTO;
import org.example.google_calendar_clone.entity.TimeEvent;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.exception.ServerErrorException;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeEventService implements IEventService<TimeEventRequest> {
    private final UserRepository userRepository;
    private final TimeEventRepository timeEventRepository;
    private final TimeEventSlotService timeEventSlotService;
    private static final Logger logger = LoggerFactory.getLogger(TimeEventService.class);

    @Override
    public UUID create(Jwt jwt, TimeEventRequest eventRequest) {
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        TimeEvent event = TimeEvent.builder()
                .startTime(eventRequest.getStartTime())
                .endTime(eventRequest.getEndTime())
                .startTimeZoneId(eventRequest.getStartTimeZoneId())
                .endTimeZoneId(eventRequest.getEndTimeZoneId())
                .repetitionFrequency(eventRequest.getRepetitionFrequency())
                .repetitionStep(eventRequest.getRepetitionStep())
                .weeklyRecurrenceDays(eventRequest.getWeeklyRecurrenceDays())
                .monthlyRepetitionType(eventRequest.getMonthlyRepetitionType())
                .repetitionDuration(eventRequest.getRepetitionDuration())
                .repetitionEndDate(eventRequest.getRepetitionEndDate())
                .repetitionOccurrences(eventRequest.getRepetitionOccurrences())
                .user(user)
                .build();
        /*
            For events that are set to be repeated for FOREVER we choose an arbitrary number like 100 years
            and set the repetition End Date to plus 100 years. We treat the event then as UNTIL_DATE but
            now the repetitionEndDate will be 100 years from now. This is 1 way to approach the FOREVER
            case
        */
        if (event.getRepetitionFrequency() != RepetitionFrequency.NEVER
                && event.getRepetitionDuration() == RepetitionDuration.FOREVER) {
            event.setRepetitionEndDate(LocalDate.from(event.getStartTime().plusYears(100)));
        }

        this.timeEventRepository.save(event);
        this.timeEventSlotService.create(eventRequest, event);

        return event.getId();
    }

    @Override
    public void deleteById(Jwt jwt, UUID id) {
        TimeEvent event = this.timeEventRepository.findByIdFetchingUser(id).orElseThrow(() ->
                new ResourceNotFoundException("Time event not found with id: " + id));
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        if (!isOrganizerOfEvent(user, event)) {
            logger.info("User with id: {} is not host of event with id: {}", user.getId(), id);
            throw new AccessDeniedException("Access Denied");
        }

        this.timeEventRepository.deleteById(id);
    }

    /*
        We need to annotate this method as @Transactional because when we call timeEvent.getUser() is called from the
        isOrganizerAtEvent(), there is no session to access the user that we retrieved from the findBy method.
        @Transactional makes the entity manager thread bound, meaning the manager/session/context/ is open for the
        methods lifecycle, and we don't have a new EntityManager/Context per db operation as it can be seen by the logs
        below. The question still remains why do we get LazyInitException when this check happens:  user.equals(dayEvent.getUser())

        The answer is because the user is a proxy created by Hibernate when we called getReferenceById(). When equals()
        get called to the proxy Hibernate tries to initialize the proxy. When @Transactional is present, we have a
        Context and, it can be initialized but in the absence of @Transactional, since the context is bounded to 1 per
        transaction and there is no active transaction, it throws. This happens despite the fact that in our equals()
        we specify only the id, which is the only present in the proxy. Hibernate is not aware of it and tries to initialize
        the proxy.

        Without @Transactional
        DEBUG org.springframework.orm.jpa.JpaTransactionManager - Creating new transaction with name [org.springframework.data.jpa.repository.support.SimpleJpaRepository.getReferenceById]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly
        DEBUG org.springframework.orm.jpa.JpaTransactionManager - Opened new EntityManager [SessionImpl(1051722121<open>)] for JPA transaction

        With @Transactional
        DEBUG org.springframework.orm.jpa.JpaTransactionManager - Found thread-bound EntityManager [SessionImpl(1577805956<open>)] for JPA transaction
        DEBUG org.springframework.orm.jpa.JpaTransactionManager - Participating in existing transaction
        TRACE org.springframework.transaction.interceptor.TransactionInterceptor - Getting transaction for [org.springframework.data.jpa.repository.support.SimpleJpaRepository.getReferenceById]
     */
    @Transactional
    public List<TimeEventSlotDTO> findEventSlotsByEventId(Jwt jwt, UUID id) {
        TimeEvent event = this.timeEventRepository.findByIdFetchingUser(id).orElseThrow(() ->
                new ResourceNotFoundException("Time event not found with id: " + id));
        User user = this.userRepository.getReferenceById(Long.valueOf(jwt.getSubject()));
        if (!isOrganizerOfEvent(user, event)) {
            logger.info("User with id: {} is not host of event with id: {}", user.getId(), id);
            throw new AccessDeniedException("Access Denied");
        }

        return this.timeEventSlotService.findEventSlotsByEventId(id);
    }

    public List<TimeEventSlotDTO> findEventSlotsByUserInDateRange(Jwt jwt, LocalDateTime startTime, LocalDateTime endTime) {
        User user = this.userRepository.findById(Long.valueOf(jwt.getSubject())).orElseThrow(() -> {
            logger.info("Authenticated user with id: {} was not found in the database", jwt.getSubject());
            return new ServerErrorException("Internal Server Error");
        });
        return this.timeEventSlotService.findEventSlotsByUserInDateRange(user, startTime, endTime);
    }

    private boolean isOrganizerOfEvent(User user, TimeEvent event) {
        return user.equals(event.getUser());
    }
}
