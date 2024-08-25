package org.example.google_calendar_clone.user.contact;

import org.example.google_calendar_clone.entity.ContactRequest;
import org.example.google_calendar_clone.entity.Role;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.entity.key.ContactRequestId;
import org.example.google_calendar_clone.exception.ContactRequestException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.role.RoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import net.datafaker.Faker;

@ExtendWith(MockitoExtension.class)
class ContactRequestServiceTest {
    @Mock
    private ContactRequestRepository contactRequestRepository;
    @Mock
    private UserContactRepository userContactRepository;
    @InjectMocks
    private ContactRequestService underTest;
    private static final Faker FAKER = new Faker();

    /*
        addContact()

        This and the test below, check that if user A added user B and user B has not accepted or rejected the request
        yet and either user A tries to add user B again, or user B tries to add user A(which in theory should not happen,
        it would have to accept or reject the pending request first, just in case). This is why in the 1st case, in the
        addContact(sender, receiver) we pass sender/receiver and in the 2nd case addContact(receiver, sender)
     */
    @Test
    void shouldThrowContactRequestExceptionWhenRequestIsAlreadyPendingFromUserAtoUserB() {
        List<User> users = createUsers();
        User sender = users.get(0);
        User receiver = users.get(1);
        ContactRequest contactRequest = createContactRequests(users, ContactRequestStatus.PENDING).get(0);

        when(this.contactRequestRepository.findContactRequestBetweenUsers(sender.getId(), receiver.getId()))
                .thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ContactRequestException.class).isThrownBy(() -> this.underTest.addContact(sender,
                receiver)).withMessage("Contact request already pending");
    }

    @Test
    void shouldThrowContactRequestExceptionWhenRequestIsAlreadyPendingFromUserBtoUserA() {
        List<User> users = createUsers();
        User sender = users.get(0);
        User receiver = users.get(1);
        ContactRequest contactRequest = createContactRequests(users, ContactRequestStatus.PENDING).get(0);

        when(this.contactRequestRepository.findContactRequestBetweenUsers(receiver.getId(), sender.getId()))
                .thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ContactRequestException.class).isThrownBy(() -> this.underTest.addContact(receiver,
                sender)).withMessage("Contact request already pending");
    }

    @Test
    void shouldThrowContactRequestExceptionWhenRequestIsAlreadyAccepted() {
        List<User> users = createUsers();
        User sender = users.get(0);
        User receiver = users.get(1);
        ContactRequest contactRequest = createContactRequests(users, ContactRequestStatus.ACCEPTED).get(0);

        when(this.contactRequestRepository.findContactRequestBetweenUsers(sender.getId(), receiver.getId()))
                .thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ContactRequestException.class).isThrownBy(() -> this.underTest.addContact(sender,
                receiver)).withMessage("Contact request already accepted");
    }

    @Test
    void shouldThrowContactRequestExceptionWhenRequestIsAlreadyRejected() {
        List<User> users = createUsers();
        User sender = users.get(0);
        User receiver = users.get(1);
        ContactRequest contactRequest = createContactRequests(users, ContactRequestStatus.REJECTED).get(0);

        when(this.contactRequestRepository.findContactRequestBetweenUsers(sender.getId(), receiver.getId()))
                .thenReturn(List.of(contactRequest));

        assertThatExceptionOfType(ContactRequestException.class).isThrownBy(() -> this.underTest.addContact(sender,
                receiver)).withMessage("Contact request already rejected");
    }

    // updateContact
    @Test
    void shouldThrowResourceNotFoundExceptionWhenContactRequestIsNotFoundToBeUpdated() {
        Long senderId = FAKER.number().numberBetween(1L, 120L);
        Long receiverId = FAKER.number().numberBetween(1L, 120L);
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.updateContact(
                        senderId,
                        receiverId,
                        ContactRequestAction.ACCEPT))
                .withMessage("Contact request was not found for sender id: " + senderId + " and receiver id: " + receiverId);
    }

    private List<User> createUsers() {
        Role role = new Role(RoleType.ROLE_VIEWER);
        User sender = new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));
        User receiver = new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));

        return List.of(sender, receiver);
    }

    private List<ContactRequest> createContactRequests(List<User> users, ContactRequestStatus status) {
        User sender = users.get(0);
        User receiver = users.get(1);

        ContactRequest contactRequest = new ContactRequest();
        contactRequest.setId(new ContactRequestId(sender.getId(), receiver.getId()));
        contactRequest.setSender(sender);
        contactRequest.setReceiver(receiver);
        contactRequest.setStatus(status);

        return List.of(contactRequest);
    }
}
