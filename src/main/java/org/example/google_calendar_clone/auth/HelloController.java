package org.example.google_calendar_clone.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
class HelloController {

    @GetMapping("/hello")
    @PreAuthorize("hasRole('ROLE_VIEWER')")
    String hello(Principal principal) {
        return "Hello " + principal.getName();
    }
}
