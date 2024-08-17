package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.DuplicateResourceException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.utils.PasswordUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

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

    private void validateUsername(String username) {
        if (username.length() > 20) {
            throw new IllegalArgumentException("Invalid username. Username must not exceed 20 characters");
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

    public User findByIdFetchingRoles(Long userId) {
        return this.userRepository.findByIdFetchingRoles(userId).orElseThrow(() ->
                new ResourceNotFoundException("User not found with id: " + userId));
    }
}
