package org.example.calendar.user.contact.request;

import org.example.calendar.entity.ContactRequest;
import org.example.calendar.entity.User;
import org.example.calendar.entity.key.ContactRequestId;
import org.assertj.core.api.AbstractAssert;

import java.time.Instant;

// failWithMessage() makes the test to fail
class ContactRequestAssert extends AbstractAssert<ContactRequestAssert, ContactRequest> {

    ContactRequestAssert(ContactRequest actual) {
        super(actual, ContactRequestAssert.class);
    }

    // This method is used to initiate the assertion chain.
    static ContactRequestAssert assertThat(ContactRequest actual) {
        return new ContactRequestAssert(actual);
    }

    // All the methods work with the actual object. isNotNull(); is called on the actual object
    ContactRequestAssert hasContactRequestId(ContactRequestId id) {
        isNotNull();
        if (!actual.getId().equals(id)) {
            failWithMessage("Expected ContactRequest to have id <%s> but was <%s>", id, actual.getId());
        }
        return this; // Returning this allows us to continue chaining
    }

    ContactRequestAssert hasSender(User sender) {
        isNotNull();
        if (!actual.getSender().equals(sender)) {
            failWithMessage("Expected ContactRequest to have sender <%s> but was <%s>", sender, actual.getSender());
        }

        return this;
    }

    ContactRequestAssert hasReceiver(User receiver) {
        isNotNull();
        if (!actual.getReceiver().equals(receiver)) {
            failWithMessage("Expected ContactRequest to have receiver <%s> but was <%s>", receiver, actual.getReceiver());
        }

        return this;
    }

    ContactRequestAssert hasStatus(ContactRequestStatus status) {
        isNotNull();
        if (!actual.getStatus().equals(status)) {
            failWithMessage("Expected ContactRequest to have status <%s> but was <%s>", status, actual.getStatus());
        }

        return this;
    }

    ContactRequestAssert wasCreatedAt(Instant createdAt) {
        isNotNull();
        if (!actual.getCreatedAt().equals(createdAt)) {
            failWithMessage("Expected ContactRequest to have a creation time before <%s> but was <%s>", createdAt, actual.getCreatedAt());
        }

        return this;
    }
}
