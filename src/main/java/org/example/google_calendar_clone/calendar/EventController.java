package org.example.google_calendar_clone.calendar;

import lombok.RequiredArgsConstructor;
import org.example.google_calendar_clone.calendar.event.dto.DayEventRequest;
import org.example.google_calendar_clone.calendar.event.dto.validator.OnCreate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {

    // https://www.baeldung.com/spring-date-parameters, https://www.baeldung.com/spring-boot-formatting-json-dates
    // https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-uri-building.html
    @PostMapping("/day-events")
    ResponseEntity<Void> createDayEvent(@Validated(OnCreate.class) @RequestBody DayEventRequest eventRequest) {
        // URI
        return  null;
    }
}
