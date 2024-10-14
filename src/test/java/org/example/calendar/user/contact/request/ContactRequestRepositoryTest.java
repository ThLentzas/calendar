package org.example.calendar.user.contact.request;

import org.example.calendar.entity.ContactRequest;
import org.example.calendar.entity.User;
import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.user.UserAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

@Import(ContactRequestRepository.class)
class ContactRequestRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ContactRequestRepository underTest;

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldCreateContactRequest() {
        ContactRequest contactRequest = ContactRequest.builder()
                .senderId(1L)
                .receiverId(4L)
                .build();
        this.underTest.create(contactRequest);

        // findPendingContactRequestsByReceiverId() is tested indirectly
        List<User> senders = this.underTest.findPendingContactRequestsByReceiverId(4L);
        List<User> users = List.of(new User(1L, "kris.hudson"), new User(2L, "clement.gulgowski"));

       /*
            For every user we are going to stream the list of senders and anyMatch() will check the current
            user with every sender in the list. If non found to match the user it will return false, match = false, we
            force the test to fail with fail(). If one of the users matches the current sender it will return true. We
            repeat the process for the other expected request. The key here is that anyMatch() will check every sender
            before returning true or false, just because the 1st does not match the sender does not mean that it will
            return false immediately.
         */
        users.forEach(user -> {
            boolean match = senders.stream()
                    .anyMatch(sender -> {
                        try {
                            UserAssert.assertThat(sender)
                                    .hasIdValue(user.getId())
                                    .hasUsername(user.getUsername());
                            return true;
                        } catch (AssertionError ae) {
                            return false;
                        }
                    });

            if (!match) {
                fail("Expected user not found: " + user);
            }
        });
    }

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldUpdateContactRequest() {
        ContactRequest contactRequest = ContactRequest.builder()
                .senderId(2L)
                .receiverId(4L)
                .status(ContactRequestStatus.ACCEPTED)
                .build();
        this.underTest.update(contactRequest);

        /*
            After updating the pending contact request the user with id 4 does not have any pending requests. Another
            assertion we could do is call findContactRequestBetweenUsers() and assert that the status now is ACCEPTED

            findPendingContactRequestBySenderAndReceiverId() is tested via the update()
         */
        List<User> senders = this.underTest.findPendingContactRequestsByReceiverId(4L);
        assertThat(senders).isEmpty();
    }

    /*
        We are testing the 2b case at db level of ContactRequestService. Based on the sql script user with id 4 added
        user with id 2, and they were rejected. User with id 2 added user with id 4 and the request is pending. In our
        sql query we only fetch the status for each request because this is the only attribute we need. The requests
        are returned in DESC order based on the createdAt attribute. We can create a list of statuses and compare the
        equivalent statuses of the fetched requests. This way we know that we have the correct order and that the expected
        records where returned based on the sql script without having to adjust our sql query and return more attributes
        like senderId, receiverId and createdAt
     */
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldFindContactRequestsBetweenUsers() {
        List<ContactRequest> requests = this.underTest.findContactRequestBetweenUsers(2L, 4L);
        List<ContactRequestStatus> statuses = List.of(ContactRequestStatus.PENDING, ContactRequestStatus.REJECTED);

        for(int i = 0; i < requests.size(); i++) {
            ContactRequestAssert.assertThat(requests.get(i))
                    .hasStatus(statuses.get(i));
        }
    }
}
