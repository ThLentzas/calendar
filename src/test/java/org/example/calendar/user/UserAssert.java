package org.example.calendar.user;

import org.assertj.core.api.AbstractAssert;
import org.example.calendar.entity.User;

public class UserAssert extends AbstractAssert<UserAssert, User> {

    UserAssert(User actual) {
        super(actual, UserAssert.class);
    }

    // This method is used to initiate the assertion chain.
    public static UserAssert assertThat(User actual) {
        return new UserAssert(actual);
    }

    // This assert is used to make sure the auto generated id is set after creating our record. We can't know the id in this case,
    // but we can still assert that it was generated and set to our record.
    UserAssert hasId() {
        isNotNull();
        if (actual.getId() == null) {
            failWithMessage("Expected User to have id but was <%s>", (Object) null);
        }
        return this;
    }

    public UserAssert hasIdValue(Long id) {
        isNotNull();
        if (!actual.getId().equals(id)) {
            failWithMessage("Expected User to have id <%s> but was <%s>", id, actual.getId());
        }
        return this;
    }

    public UserAssert hasUsername(String username) {
        isNotNull();
        if (!actual.getUsername().equals(username)) {
            failWithMessage("Expected User to have username <%s> but was <%s>", username, actual.getUsername());
        }
        return this;
    }

    public UserAssert hasEmail(String email) {
        isNotNull();
        if (!actual.getEmail().equals(email)) {
            failWithMessage("Expected User to have email <%s> but was <%s>", email, actual.getEmail());
        }
        return this;
    }
}
