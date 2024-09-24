package org.example.calendar.user;

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
import org.example.calendar.user.contact.dto.CreateContactRequest;
import org.example.calendar.user.contact.dto.PendingContactRequest;
import org.example.calendar.user.contact.dto.UpdateContactRequest;
import org.example.calendar.user.dto.UserProfile;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

import java.util.List;

/*
     IMPORTANT!!! We don't pass the Jwt in the service to extract the userId. Service layer should not know anything
     about jwt/auth mechanism.
 */
@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

    @PostMapping("/contacts")
    ResponseEntity<Void> sendContactRequest(@Valid @RequestBody CreateContactRequest contactRequest,
                                            @AuthenticationPrincipal Jwt jwt) {
        Long senderId = Long.valueOf(jwt.getSubject());
        this.userService.sendContactRequest(contactRequest, senderId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
        Retrieves the pending contact requests for the current user
     */
    @GetMapping("/contact-requests")
    ResponseEntity<List<PendingContactRequest>> findPendingContactsRequests(@AuthenticationPrincipal Jwt jwt) {
        Long receiverId = Long.valueOf(jwt.getSubject());
        List<PendingContactRequest> contactsRequests = this.userService.findPendingContactRequests(receiverId);

        return new ResponseEntity<>(contactsRequests, HttpStatus.OK);
    }

    @PutMapping("/contact-requests")
    ResponseEntity<Void> updateContactRequest(@Valid @RequestBody UpdateContactRequest contactRequest,
                                              @AuthenticationPrincipal Jwt jwt) {
        Long receiverId = Long.valueOf(jwt.getSubject());
        this.userService.updateContactRequest(contactRequest, receiverId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/contacts")
    ResponseEntity<List<UserProfile>> findContacts(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        List<UserProfile> contacts = this.userService.findContacts(userId);

        return new ResponseEntity<>(contacts, HttpStatus.OK);
    }
}
