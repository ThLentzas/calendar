package org.example.google_calendar_clone.calendar;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.example.google_calendar_clone.calendar.dto.DayEventRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriBuilder;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class EventController {

    // https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-uri-building.html
    @PostMapping("/day-events")
    ResponseEntity<Void> createDayEvent(@Valid @RequestBody DayEventRequest eventRequest) {
        // URI
        return  null;
    }
}
