package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.entity.ContactRequest;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.DuplicateResourceException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.contact.ContactService;
import org.example.google_calendar_clone.user.contact.request.ContactRequestService;
import org.example.google_calendar_clone.user.contact.dto.CreateContactRequest;
import org.example.google_calendar_clone.user.contact.dto.PendingContactRequest;
import org.example.google_calendar_clone.user.contact.dto.UpdateContactRequest;
import org.example.google_calendar_clone.user.dto.UserProfile;
import org.example.google_calendar_clone.user.dto.UserProfileConverter;
import org.example.google_calendar_clone.utils.PasswordUtils;
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
    private final UserRepository userRepository;
    private final UserProfileConverter profileConverter = new UserProfileConverter();
    private final PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
        validateUsername(user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());

        if (this.userRepository.existsByEmailIgnoringCase(user.getEmail())) {
            throw new DuplicateResourceException("The provided email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.userRepository.save(user);
    }

    /*
        The current user(sender) makes a request to add another registered user to their contacts
     */
    @Transactional
    void sendContactRequest(CreateContactRequest contactRequest, Long senderId) {
        User sender = this.userRepository.getReferenceById(senderId);
        User receiver = this.userRepository.findById(contactRequest.receiverId()).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + contactRequest.receiverId()));

        this.contactRequestService.sendContactRequest(sender, receiver);
    }

    /*
        The current user(receiver) received a contact request and either accepts or rejects the request
     */
    void updateContactRequest(UpdateContactRequest contactRequest, Long receiverId) {
        Long senderId = contactRequest.senderId();
        this.contactRequestService.updateContactRequest(senderId, receiverId, contactRequest.action());
    }

    public User findByIdFetchingRoles(Long userId) {
        return this.userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + userId));
    }

    List<PendingContactRequest> findPendingContactRequests(Long receiverId) {
        List<ContactRequest> contactRequests = this.contactRequestService.findPendingContacts(receiverId);
        return contactRequests.stream()
                .map(contactRequest -> new PendingContactRequest(
                        this.profileConverter.convert(contactRequest.getSender()), contactRequest.getStatus()))
                .toList();
    }

    List<UserProfile> findContacts(Long userId) {
        List<User> users = this.contactService.findContacts(userId);

        return users.stream()
                .map(this.profileConverter::convert)
                .toList();
    }

    private void validateUsername(String username) {
        if (username.length() > 20) {
            throw new IllegalArgumentException("Invalid username. Username must not exceed 20 characters");
        }

        if(!username.matches("^[a-zA-Z0-9.]*$")) {
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
