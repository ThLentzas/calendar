package org.example.google_calendar_clone.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.example.google_calendar_clone.user.contact.dto.CreateContactRequest;
import org.example.google_calendar_clone.user.contact.dto.PendingContactRequest;
import org.example.google_calendar_clone.user.contact.dto.UpdateContactRequest;
import org.example.google_calendar_clone.user.dto.UserProfile;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

import java.util.List;

/*
    We only give access to @PreAuthorize("hasRole('ROLE_VIEWER')") because to have any other role, the user has by default
    the ROLE_VIEWER upon registration
 */
@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

    @PostMapping("/contacts")
    @PreAuthorize("hasRole('ROLE_VIEWER')")
    ResponseEntity<Void> sendContactRequest(@Valid @RequestBody CreateContactRequest contactRequest,
                                            // The principal of the JwtAuthenticationToken is a Jwt
                                            @AuthenticationPrincipal Jwt jwt) {
        this.userService.sendContactRequest(contactRequest, jwt);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
        Retrieves the pending contact requests for the current user
     */
    @GetMapping("/contact-requests")
    @PreAuthorize("hasRole('ROLE_VIEWER')")
    ResponseEntity<List<PendingContactRequest>> findPendingContactsRequests(@AuthenticationPrincipal Jwt jwt) {
        List<PendingContactRequest> contactsRequests = this.userService.findPendingContactsRequests(jwt);

        return new ResponseEntity<>(contactsRequests, HttpStatus.OK);
    }

    @PutMapping("/contact-requests")
    @PreAuthorize("hasRole('ROLE_VIEWER')")
    ResponseEntity<Void> updateContactRequest(@Valid @RequestBody UpdateContactRequest contactRequest,
                                              @AuthenticationPrincipal Jwt jwt) {
        this.userService.updateContactRequest(contactRequest, jwt);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/contacts")
    @PreAuthorize("hasRole('ROLE_VIEWER')")
    ResponseEntity<List<UserProfile>> findContacts(@AuthenticationPrincipal Jwt jwt) {
        List<UserProfile> contacts = this.userService.findContacts(jwt);

        return new ResponseEntity<>(contacts, HttpStatus.OK);
    }
    // toDo: delete user and access token set to ""
}
