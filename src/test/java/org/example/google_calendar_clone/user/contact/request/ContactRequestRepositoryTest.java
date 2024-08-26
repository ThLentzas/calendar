package org.example.google_calendar_clone.user.contact.request;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.entity.ContactRequest;
import org.example.google_calendar_clone.entity.Role;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.entity.key.ContactRequestId;
import org.example.google_calendar_clone.role.RoleRepository;
import org.example.google_calendar_clone.role.RoleType;
import org.example.google_calendar_clone.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.Fail.fail;

import net.datafaker.Faker;

/*
    IMPORTANT!!!!!! This class and the UserRepositoryTest class use methods for setting up data. The ContactRepositoryTest
    uses sql scripts.

    The reason why we call this.testEntityManager.flush(); in both createUsers() and createContactRequests() is that both
    methods are called from a method annotated with @Test and those methods are @DataJpaTest methods which means that
    they are transactional. If we don't call this.testEntityManager.flush();, the reads that we try, our repo methods
    will look at the Context and fetch those entities from there, because the changes are not flushed to the db.

    https://github.com/spring-projects/spring-boot/issues/11183
    https://stackoverflow.com/questions/64459905/persist-many-to-many-relationship-with-datajpatest-and-h2-database-not-working
    https://stackoverflow.com/questions/72020788/field-annotated-with-transient-being-persisted-in-datajpatest
 */
class ContactRequestRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ContactRequestRepository underTest;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TestEntityManager testEntityManager;
    private static final Faker FAKER = new Faker();

    @Test
    void shouldFindContactRequestBetweenUsers() {
        List<User> users = createUsers();
        User sender = users.get(0);
        User receiver = users.get(2);
        List<ContactRequest> requests = createContactRequests(users);
        /*
            If we simply call List<ContactRequest> expected = createContactRequests(users);, when we loop we will have
            3 requests and only 2 actually match so 1 of the expected requests will be unmatched and the test will fail
            The requests that are between sender = users.get(0); and receiver = users.get(2); are the 0 and 1.
         */
        List<ContactRequest> expected = List.of(requests.get(0), requests.get(1));
        List<ContactRequest> actual = this.underTest.findContactRequestBetweenUsers(sender.getId(), receiver.getId());

        /*
            It's not straight forward to assert on the list, because the order of retrieval is not guaranteed to be the
            same as the order of insertion.

            For every expectedRequest we are going to stream the actual list and anyMatch() will check the current
            expected request with both the actual requests. If non found to match the expected request it will return
            false, match = false, we force the test to fail with fail(). If one of the actual requests matches the
            current expected request it will return true. We repeat the process for the other expected request. The key
            here is that anyMatch() will check both actual requests before returning true or false, just because the 1st
            does not match the expected request does not mean that it will return false immediately.
         */
        expected.forEach(expectedRequest -> {
            boolean match = actual.stream()
                    .anyMatch(actualRequest -> {
                        try {
                            ContactRequestAssert.assertThat(actualRequest)
                                    .hasContactRequestId(expectedRequest.getId())
                                    .hasSender(expectedRequest.getSender())
                                    .hasReceiver(expectedRequest.getReceiver());
                            return true;
                        } catch (AssertionError ae) {
                            return false;
                        }
                    });

            if (!match) {
                // forces the test to fail
                fail("Expected contact request not found: " + expectedRequest);
            }
        });
    }

    @Test
    void shouldFindContactRequestBySenderAndReceiverId() {
        List<User> users = createUsers();
        User sender = users.get(0);
        User receiver = users.get(2);
        ContactRequest expected = createContactRequests(users).get(1);

        this.underTest.findPendingContactRequestBySenderAndReceiverId(sender.getId(),
                receiver.getId(), ContactRequestStatus.PENDING).ifPresent(actual ->
                ContactRequestAssert.assertThat(actual)
                        .hasContactRequestId(expected.getId())
                        .hasSender(sender)
                        .hasReceiver(receiver)
                        .hasStatus(expected.getStatus()));
    }

    @Test
    void shouldFindPendingContactRequestsByReceiverIdOrderByCreatedDateDesc() {
        List<User> users = createUsers();
        User receiver = users.get(2);
        List<ContactRequest> requests = createContactRequests(users);

        List<ContactRequest> expectedRequests = List.of(requests.get(1), requests.get(2));
        List<ContactRequest> actualRequests = this.underTest.findPendingContactRequestsByReceiverId(receiver.getId());

        expectedRequests.forEach(expectedRequest -> {
            boolean match = actualRequests.stream()
                    .anyMatch(actualRequest -> {
                        try {
                            ContactRequestAssert.assertThat(actualRequest)
                                    .hasContactRequestId(expectedRequest.getId())
                                    .hasSender(expectedRequest.getSender())
                                    .hasReceiver(expectedRequest.getReceiver())
                                    .hasStatus(expectedRequest.getStatus())
                                    .wasCreatedAt(expectedRequest.getCreatedAt());
                            return true;
                        } catch (AssertionError ae) {
                            return false;
                        }
                    });

            if (!match) {
                fail("Expected contact request not found: " + expectedRequest);
            }
        });

        assertThat(actualRequests).isSortedAccordingTo(Comparator.comparing(ContactRequest::getCreatedAt).reversed());
    }

    private List<User> createUsers() {
        // We don't have to create a role because there roles are present from the sql scripts that Flyway migrated
        Role role = this.roleRepository.findByRoleType(RoleType.ROLE_VIEWER).orElseThrow();
        User sender1 = new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));
        User sender2 = new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));
        User receiver = new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));

        this.userRepository.save(sender1);
        this.userRepository.save(sender2);
        this.userRepository.save(receiver);
        this.testEntityManager.flush();

        return List.of(sender1, sender2, receiver);
    }

    /*
        ContactRequest rejectedContactRequest = new ContactRequest();
        rejectedContactRequest.setId(new ContactRequestId(sender.getId(), receiver.getId()));
        rejectedContactRequest.setSender(sender);
        rejectedContactRequest.setReceiver(receiver);
        rejectedContactRequest.setStatus(ContactRequestStatus.REJECTED);
        ContactRequest pendingContactRequest = new ContactRequest();
        pendingContactRequest.setId(new ContactRequestId(sender.getId(), receiver.getId()));
        pendingContactRequest.setSender(receiver);
        pendingContactRequest.setReceiver(sender);
        pendingContactRequest.setStatus(ContactRequestStatus.PENDING);

        this.underTest.save(rejectedContactRequest);
        this.underTest.save(pendingContactRequest);
        this.testEntityManager.flush();

        The above code produces the error below why? Why is there an update query where it causes an exception because
        created at is null?

        The error is caused by the creation of composite PK for the 2 contact requests.
            rejectedContactRequest.setId(new ContactRequestId(sender.getId(), receiver.getId()));
            pendingContactRequest.setId(new ContactRequestId(sender.getId(), receiver.getId()));
        As we can see, both the ids will end up being the same, so while Hibernate inserts the 1st time the request
        the 2nd time, it sees that there is already a ContactRequest with the same PK and tries to update it. This is a
        logical that is explained in the code below.

        Hibernate:
        insert
        into
            contact_requests
            (created_at, status, updated_at, receiver_id, sender_id)
        values
            (?, ?, ?, ?, ?)

        Hibernate:
            update
                contact_requests
            set
                created_at=?,
                status=?,
                updated_at=?
            where
                receiver_id=?
                and sender_id=?

        org.hibernate.exception.ConstraintViolationException: could not execute statement [ERROR: null value in column
        "created_at" of relation "contact_requests" violates not-null constraint

     */
    private List<ContactRequest> createContactRequests(List<User> users) {
        User sender1 = users.get(0);
        User sender2 = users.get(1);
        User receiver = users.get(2);

        /*
            We create 2 contact requests for the same pair of users to test the case where user A rejected user's B
            contact request and, then user B sent a contact request to user A. In the 1st case the status is REJECTED
            and in the 2nd one is PENDING.
         */
        ContactRequest rejectedContactRequest = new ContactRequest();
        rejectedContactRequest.setId(new ContactRequestId(receiver.getId(), sender1.getId()));
        rejectedContactRequest.setSender(receiver);
        rejectedContactRequest.setReceiver(sender1);
        rejectedContactRequest.setStatus(ContactRequestStatus.REJECTED);
        ContactRequest pendingContactRequest = new ContactRequest();
        pendingContactRequest.setId(new ContactRequestId(sender1.getId(), receiver.getId()));
        pendingContactRequest.setSender(sender1);
        pendingContactRequest.setReceiver(receiver);
        pendingContactRequest.setStatus(ContactRequestStatus.PENDING);
        ContactRequest contactRequest1 = new ContactRequest();
        contactRequest1.setId(new ContactRequestId(sender2.getId(), receiver.getId()));
        contactRequest1.setSender(sender2);
        contactRequest1.setReceiver(receiver);
        contactRequest1.setStatus(ContactRequestStatus.PENDING);

        /*
            Since the ContactRequest uses an EmbeddedId, save() calls merge() and not persist(). merge() returns the
            managed entity and does not update the reference as persist() does. We need to add that to have the
            dates with values so, we can assert on the createdAt. The date is generated the moment the entity is managed
            by Hibernate not when the INSERT happens during flush().

            Flow:
                save()
                save internally sees that ID(isNew()) in the object exists so it calls merge() and not persist()
                merge() looks for an object of the same ID in the persistence context but fails
                merge() looks for an object of the same ID in the database but fails
                merge() decides that the object is new, creates a new entity and copies everything to it from the fields of our object
                merge() returns the reference to the new object which save() also returns
         */
        rejectedContactRequest = this.underTest.save(rejectedContactRequest);
        pendingContactRequest = this.underTest.save(pendingContactRequest);
        contactRequest1 = this.underTest.save(contactRequest1);
        this.testEntityManager.flush();

        return List.of(rejectedContactRequest, pendingContactRequest, contactRequest1);
    }
}
