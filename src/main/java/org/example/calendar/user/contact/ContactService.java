package org.example.calendar.user.contact;

import org.example.calendar.entity.Contact;
import org.example.calendar.entity.ContactRequest;
import org.example.calendar.entity.User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {
    private final ContactRepository repository;

    /*
        In the contacts table, we only create a single entry that will represent both sides of the relationship, so
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
    public void createContact(ContactRequest contactRequest) {
        Long userId1 = contactRequest.getSenderId() < contactRequest.getReceiverId() ? contactRequest.getSenderId() : contactRequest.getReceiverId();
        Long userId2 = contactRequest.getSenderId() > contactRequest.getReceiverId() ? contactRequest.getSenderId() : contactRequest.getReceiverId();
        Contact contact = new Contact(userId1, userId2);
        this.repository.create(contact);
    }

    public List<User> findContacts(Long userId) {
        return this.repository.findContacts(userId);
    }
}
