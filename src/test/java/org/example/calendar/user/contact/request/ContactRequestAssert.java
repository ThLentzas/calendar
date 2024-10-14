package org.example.calendar.user.contact.request;

import org.example.calendar.entity.ContactRequest;
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

    ContactRequestAssert hasSender(Long senderId) {
        isNotNull();
        if (!actual.getSenderId().equals(senderId)) {
            failWithMessage("Expected ContactRequest to have sender with id <%s> but was <%s>", senderId, actual.getSenderId());
        }

        return this;
    }

    ContactRequestAssert hasReceiver(Long receiverId) {
        isNotNull();
        if (!actual.getReceiverId().equals(receiverId)) {
            failWithMessage("Expected ContactRequest to have receiver with id <%s> but was <%s>", receiverId, actual.getReceiverId());
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
