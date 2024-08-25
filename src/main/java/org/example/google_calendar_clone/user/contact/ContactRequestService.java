package org.example.google_calendar_clone.user.contact;

import org.example.google_calendar_clone.entity.ContactRequest;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.entity.UserContact;
import org.example.google_calendar_clone.entity.key.ContactRequestId;
import org.example.google_calendar_clone.entity.key.UserContactId;
import org.example.google_calendar_clone.exception.ContactRequestException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactRequestService {
    private final ContactRequestRepository contactRequestRepository;
    private final UserContactRepository userContactRepository;

    /*
        If user A adds user B, then we create a relationship with sender: A, receiver: B, and status: PENDING.
        Now, if user B tries to add user A, a query like WHERE c.sender.id = :senderId AND c.receiver.id = :receiverId
        alone would not return a row because the sender is B and the receiver is A. In practice, this is not possible
        because when user B tries to add user A, they will see that there is a request pending, and they can either
        accept or reject it. We have to check for the existing request in either direction (sender = A and receiver = B,
        or sender = B and receiver = A) before creating a new request.
        WHERE (c.sender.id = :senderId AND c.receiver.id = :receiverId) OR (c.sender.id = :receiverId AND c.receiver.id = :senderId)

        Case 1: Contact request is found and is on PENDING state, either User A has already sent a request to User B, or
        User B is trying to send a request to User A while the initial request is still pending. We don't allow it.
        Case 2: Contact request is found and is on REJECTED state
            a: If User A is trying to send another request to User B,
        who previously rejected it, we don't allow the request, as User A is trying to resend a request that was rejected
        by User B.
            b: If User B, who previously rejected a request from User A, is now trying to send a request to User A, we
            allow the request, as User B is now initiating the contact after previously rejecting User A
        Case 3: Contact request is found and is on ACCEPTED state, it means that either User A or User B is trying to
        send a new request, but they are already connected

        In any other case, either there is no relationship between users A, B or Case 2b we create a new record.

        Why do we return a List? In the case of 2b, we are going to have the following records
            senderId: A.id, receiverId: B.id, status: REJECTED
            senderId: B.id, receiverId: A.id, status: PENDING
        which means that our findContactRequestBetweenUsers() will fetch them both.
     */
    public void addContact(User sender, User receiver) {
        List<ContactRequest> contactRequests = this.contactRequestRepository.findContactRequestBetweenUsers(
                sender.getId(),
                receiver.getId());
        for (ContactRequest contactRequest : contactRequests) {
            switch (contactRequest.getStatus()) {
                case PENDING -> throw new ContactRequestException("Contact request already pending");
                case REJECTED -> {
                    if (contactRequest.getSender().equals(sender)) {
                        throw new ContactRequestException("Contact request already rejected");
                    }
                }
                case ACCEPTED -> throw new ContactRequestException("Contact request already accepted");
            }
        }

        ContactRequest contactRequest = new ContactRequest();
        contactRequest.setId(new ContactRequestId(sender.getId(), receiver.getId()));
        contactRequest.setSender(sender);
        contactRequest.setReceiver(receiver);
        contactRequest.setStatus(ContactRequestStatus.PENDING);
        contactRequestRepository.save(contactRequest);
    }

    @Transactional
    public void updateContact(Long senderId, Long receiverId, ContactRequestAction action) {
        ContactRequest contactRequest = this.contactRequestRepository.findPendingContactRequestBySenderAndReceiverId(
                senderId, receiverId, ContactRequestStatus.PENDING).orElseThrow(() -> new ResourceNotFoundException(
                "Contact request was not found for sender id: " + senderId + " and receiver id: " + receiverId)
        );

        // Switch requires at least 3 cases
        if (action.equals(ContactRequestAction.ACCEPT)) {
            contactRequest.setStatus(ContactRequestStatus.ACCEPTED);
            createUserContact(contactRequest);
        } else {
            contactRequest.setStatus(ContactRequestStatus.REJECTED);
        }
        this.contactRequestRepository.save(contactRequest);
    }

    public List<ContactRequest> findPendingContacts(Long receiverId) {
        return this.contactRequestRepository.findPendingContactRequestsByReceiverId(receiverId);
    }

    /*
        In the user_contacts table, we only create a single entry that will represent both sides of the relationship, so
        if we store user_id_1: 1, user_id_2: 2 it is bidirectional. If instead actually stored the relationship to show
        that it is bidirectional, user_id_1: 1, user_id_2: 2 and user_id_1: 2, user_id_2: 1 we would consume twice the
        space and, also we have to be careful with consistency because any change to one of the two records has to be
        applied to the other one. In the 1st case, we just have to query both directions when retrieving the contacts
        for a user.

        An issue that we have to deal with is since our PK is the composite key of (user_id_1: 1, user_id_2: 2) and in
        theory this is the same as (user_id_1: 2, user_id_2: 1), so we need a way to avoid adding a record in the 2nd case.
        To do that we force that ids are entered in a sorted so that user_id_1 < user_id_2 and (1, 2) and (2, 1) will
        be (1, 2) and we can not insert the same PK twice. This situation will not happen because we have already added
        logic in the addContact() method to prevent this scenario it's just an extra measurement in case our logic is
        correct. If user A tries to add user B and user B tries to add user A, it would result in an Exception as mentioned
        above.

        https://www.bitbytebit.xyz/p/user-friends-system-and-database
     */
    private void createUserContact(ContactRequest contactRequest) {
        User user1 = contactRequest.getSender().getId() < contactRequest.getReceiver().getId() ?
                contactRequest.getSender() : contactRequest.getReceiver();
        User user2 = contactRequest.getSender().getId() > contactRequest.getReceiver().getId() ?
                contactRequest.getSender() : contactRequest.getReceiver();

        UserContact userContact = new UserContact();
        userContact.setId(new UserContactId(user1.getId(), user2.getId()));
        userContact.setUser1(user1);
        userContact.setUser2(user2);
        this.userContactRepository.save(userContact);
    }
}
