package org.example.google_calendar_clone.user;

import org.assertj.core.api.AbstractAssert;
import org.example.google_calendar_clone.entity.Role;
import org.example.google_calendar_clone.entity.User;

import java.util.Set;

class UserAssert extends AbstractAssert<UserAssert, User> {

    UserAssert(User actual) {
        super(actual, UserAssert.class);
    }

    // This method is commonly used to initiate the assertion chain.
    static UserAssert assertThat(User actual) {
        return new UserAssert(actual);
    }

    // All the methods work with the actual object. isNotNull(); is called on the actual object
    UserAssert hasUsername(String username) {
        isNotNull();
        if(!actual.getUsername().equals(username)) {
            failWithMessage("Expected username to be <%s> but was <%s>", username, actual.getUsername());
        }

        return this; // Returning this allows us to continue chaining
    }

    UserAssert hasEmailIgnoringCase(String email) {
        isNotNull();
        if(!actual.getEmail().equalsIgnoreCase(email)) {
            failWithMessage("Expected email to be <%s> but was <%s>", email, actual.getEmail());
        }

        return this; // Returning this allows us to continue chaining
    }

    UserAssert hasPassword(String password) {
        isNotNull();
        if (!actual.getPassword().equals(password)) {
            failWithMessage("Expected password to be <%s> but was <%s>", password, actual.getPassword());
        }

        return this; // Returning this allows us to continue chaining
    }

    UserAssert hasRoles(Set<Role> roles) {
        isNotNull();
        if (!actual.getRoles().containsAll(roles)) {
            failWithMessage("Expected user's roles to be <%s> but were <%s>", roles, actual.getRoles());
        }

        return this; // Returning this allows us to continue chaining
    }
}
