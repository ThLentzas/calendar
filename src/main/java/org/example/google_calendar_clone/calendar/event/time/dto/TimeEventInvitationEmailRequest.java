package org.example.google_calendar_clone.calendar.event.time.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.example.google_calendar_clone.calendar.event.AbstractEventInvitationEmailRequest;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TimeEventInvitationEmailRequest extends AbstractEventInvitationEmailRequest {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId startTimeZoneId;
    private ZoneId endTimeZoneId;
}
