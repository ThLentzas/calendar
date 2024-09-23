package org.example.google_calendar_clone.user.contact.request;

import org.example.google_calendar_clone.entity.ContactRequest;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.entity.key.ContactRequestId;
import org.example.google_calendar_clone.exception.ConflictException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.contact.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactRequestService {
    private final ContactRequestRepository contactRequestRepository;
    private final ContactService contactService;

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
    public void sendContactRequest(User sender, User receiver) {
        List<ContactRequest> contactRequests = this.contactRequestRepository.findContactRequestBetweenUsers(sender.getId(), receiver.getId());
        for (ContactRequest contactRequest : contactRequests) {
            switch (contactRequest.getStatus()) {
                case PENDING -> throw new ConflictException("Contact request already pending");
                case REJECTED -> {
                    if (contactRequest.getSender().equals(sender)) {
                        throw new ConflictException("Contact request already rejected");
                    }
                }
                case ACCEPTED -> throw new ConflictException("Contact request already accepted");
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
    public void updateContactRequest(Long senderId, Long receiverId, ContactRequestAction action) {
        ContactRequest contactRequest = this.contactRequestRepository.findPendingContactRequestBySenderAndReceiverId(senderId, receiverId, ContactRequestStatus.PENDING).orElseThrow(() -> new ResourceNotFoundException("Contact request was not found for sender id: " + senderId + " and receiver id: " + receiverId));
        // Switch requires at least 3 cases
        if (action.equals(ContactRequestAction.ACCEPT)) {
            contactRequest.setStatus(ContactRequestStatus.ACCEPTED);
            this.contactService.createContact(contactRequest);
        } else {
            contactRequest.setStatus(ContactRequestStatus.REJECTED);
        }
        this.contactRequestRepository.save(contactRequest);
    }

    public List<ContactRequest> findPendingContacts(Long receiverId) {
        return this.contactRequestRepository.findPendingContactRequestsByReceiverId(receiverId);
    }
}
