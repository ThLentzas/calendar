package org.example.google_calendar_clone.calendar.event.day.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.example.google_calendar_clone.calendar.event.AbstractEventInvitationEmailRequest;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class DayEventInvitationEmailRequest extends AbstractEventInvitationEmailRequest {
    private LocalDate startDate;
}
