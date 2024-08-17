package org.example.google_calendar_clone.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.google_calendar_clone.auth.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    ResponseEntity<Void> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                      HttpServletResponse httpServletResponse) {
        this.authService.registerUser(registerRequest, httpServletResponse);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    ResponseEntity<Void> refresh(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.authService.refresh(servletRequest, servletResponse);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
        This endpoint will set the CSRF token as a Cookie in the response so, we could use it in subsequent requests.
     */
    @GetMapping("/csrf")
    ResponseEntity<Void> csrf() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
