package org.example.calendar.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.example.calendar.auth.dto.RegisterRequest;

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

    /*
        When this request is made, it means that a previous request was made with an invalid jwt(expired or malformed)
        The server set an "AccessToken" cookie in the response with the value = "" and maxAge = 0 so the browser would
        remove the invalid token. There is no access token to be sent with the /refresh request, even if the path of
        the access token is set to /
     */
    @PostMapping("/token/refresh")
    ResponseEntity<Void> refresh(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.authService.refresh(servletRequest, servletResponse);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
        When a user logs out we are revoking their access token by setting a Cookie with name "ACCESS_TOKEN" value = ""
        and maxAge = 0 so the browser will not include it in subsequent requests and remove it. We do the same for the
        refresh token.
     */
    @PostMapping("/token/revoke")
    ResponseEntity<Void> revoke(HttpServletResponse servletResponse) {
        this.authService.revoke(servletResponse);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // This endpoint will set the CSRF token as a Cookie in the response so, we could use it in subsequent requests.
    @GetMapping("/token/csrf")
    ResponseEntity<Void> csrf() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
