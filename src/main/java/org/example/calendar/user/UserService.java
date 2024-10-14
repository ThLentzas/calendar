package org.example.calendar.user;

import org.example.calendar.entity.User;
import org.example.calendar.exception.DuplicateResourceException;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.contact.ContactService;
import org.example.calendar.user.contact.request.ContactRequestService;
import org.example.calendar.user.contact.dto.CreateContactRequest;
import org.example.calendar.user.contact.dto.PendingContactRequest;
import org.example.calendar.user.contact.dto.UpdateContactRequest;
import org.example.calendar.user.contact.request.ContactRequestStatus;
import org.example.calendar.user.dto.UserProfile;
import org.example.calendar.user.dto.UserProfileConverter;
import org.example.calendar.utils.PasswordUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final ContactRequestService contactRequestService;
    private final ContactService contactService;
    private final UserRepository repository;
    private static final UserProfileConverter CONVERTER = new UserProfileConverter();
    private final PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
        validateUsername(user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());

        if (this.repository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("The provided email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.repository.create(user);
    }

    /*
        The current user(sender) makes a request to add another registered user to their contacts
     */
    @Transactional
    void sendContactRequest(CreateContactRequest contactRequest, Long senderId) {
        User receiver = this.repository.findById(contactRequest.receiverId()).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + contactRequest.receiverId()));
        this.contactRequestService.sendContactRequest(senderId, receiver.getId());
    }

    /*
        The current user(receiver) received a contact request and either accepts or rejects the request
     */
    void updateContactRequest(UpdateContactRequest contactRequest, Long receiverId) {
        this.contactRequestService.updatePendingContactRequest(contactRequest.senderId(), receiverId, contactRequest.action());
    }

    public User findById(Long userId) {
        return this.repository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    List<PendingContactRequest> findPendingContactRequests(Long receiverId) {
        List<User> users = this.contactRequestService.findPendingContacts(receiverId);
        return users.stream()
                .map(user -> new PendingContactRequest(CONVERTER.convert(user), ContactRequestStatus.PENDING))
                .toList();
    }

    List<UserProfile> findContacts(Long userId) {
        List<User> users = this.contactService.findContacts(userId);
        return users.stream()
                .map(CONVERTER::convert)
                .toList();
    }

    private void validateUsername(String username) {
        if (username.length() > 20) {
            throw new IllegalArgumentException("Invalid username. Username must not exceed 20 characters");
        }

        if (!username.matches("^[a-zA-Z0-9.]*$")) {
            throw new IllegalArgumentException("Invalid username. Username should contain only characters, numbers and .");
        }
    }

    private void validatePassword(String password) {
        PasswordUtils.validatePassword(password);
    }

    private void validateEmail(String email) {
        if (email.length() > 50) {
            throw new IllegalArgumentException("Invalid email. Email must not exceed 50 characters");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}
