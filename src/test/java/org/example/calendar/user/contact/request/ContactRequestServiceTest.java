package org.example.calendar.user.contact.request;

import org.example.calendar.entity.ContactRequest;
import org.example.calendar.exception.ConflictException;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.contact.ContactService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import net.datafaker.Faker;

@ExtendWith(MockitoExtension.class)
class ContactRequestServiceTest {
    @Mock
    private ContactRequestRepository repository;
    @Mock
    private ContactService contactService;
    @InjectMocks
    private ContactRequestService underTest;
    private static final Faker FAKER = new Faker();

    /*
        sendContactRequest()

        Maybe this test is not needed? All we assert is that the call to the repository is made. The repository method
        is already tested.
     */
    @Test
    void shouldSendContactRequest() {
        ContactRequest contactRequest = ContactRequest.builder()
                .senderId(1L)
                .receiverId(2L)
                .status(ContactRequestStatus.PENDING)
                .build();
        when(this.repository.findContactRequestBetweenUsers(1L, 2L)).thenReturn(Collections.emptyList());
        doNothing().when(this.repository).create(contactRequest);

        this.underTest.sendContactRequest(1L, 2L);

        verify(this.repository, times(1)).create(contactRequest);
    }

    // sendContactRequest()
    @Test
    void shouldThrowConflictExceptionWhenRequestIsAlreadyPending() {
        ContactRequest contactRequest = ContactRequest.builder()
                .senderId(1L)
                .receiverId(2L)
                .status(ContactRequestStatus.PENDING)
                .build();

        when(this.repository.findContactRequestBetweenUsers(1L, 2L)).thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.sendContactRequest(1L, 2L)).withMessage("Contact request already pending");
    }

    // sendContactRequest()
    @Test
    void shouldThrowConflictExceptionWhenRequestIsAlreadyAccepted() {
        ContactRequest contactRequest = ContactRequest.builder()
                .senderId(1L)
                .receiverId(2L)
                .status(ContactRequestStatus.ACCEPTED)
                .build();

        when(this.repository.findContactRequestBetweenUsers(1L, 2L)).thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.sendContactRequest(1L, 2L)).withMessage("Contact request already accepted");
    }

    // sendContactRequest()
    @Test
    void shouldThrowContactRequestExceptionWhenRequestIsAlreadyRejected() {
        ContactRequest contactRequest = ContactRequest.builder()
                .senderId(1L)
                .receiverId(2L)
                .status(ContactRequestStatus.REJECTED)
                .build();

        when(this.repository.findContactRequestBetweenUsers(1L, 2L)).thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() -> this.underTest.sendContactRequest(1L, 2L)).withMessage("Contact request already rejected");
    }

    // updateContact()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenContactRequestIsNotFoundToBeUpdated() {
        Long senderId = FAKER.number().numberBetween(1L, 120L);
        Long receiverId = FAKER.number().numberBetween(1L, 120L);
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.updatePendingContactRequest(senderId, receiverId, ContactRequestAction.ACCEPT)).withMessage("Contact request was not found for sender id: " + senderId + " and receiver id: " + receiverId);
    }
}
