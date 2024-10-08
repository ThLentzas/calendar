package org.example.calendar.event.day.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.example.calendar.event.AbstractEventInvitationRequest;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class DayEventInvitationRequest extends AbstractEventInvitationRequest {
    private LocalDate startDate;
}
