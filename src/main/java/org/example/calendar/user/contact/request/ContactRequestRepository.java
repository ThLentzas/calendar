package org.example.calendar.user.contact.request;

import org.example.calendar.entity.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.example.calendar.entity.ContactRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ContactRequestRepository {
    private final JdbcClient jdbcClient;

    /*
        We don't have to check for a DuplicateKey. Our composite PK will be unique because we handle the case in the
        ContactRequestService.
     */
    void create(ContactRequest contactRequest) {
        this.jdbcClient.sql("""
                            INSERT INTO contact_requests
                            VALUES (:senderId, :receiverId, :status::contact_request_status, :createdAt, :updatedAt)
                        """)
                .param("senderId", contactRequest.getSenderId())
                .param("receiverId", contactRequest.getReceiverId())
                .param("status", ContactRequestStatus.PENDING.name())
                // UTC times by default
                .param("createdAt", Timestamp.from(Instant.now()))
                .param("updatedAt", Timestamp.from(Instant.now()))
                .update();
    }

    void update(ContactRequest contactRequest) {
        this.jdbcClient.sql("""
                            UPDATE contact_requests
                            SET status = :status::contact_request_status, updated_at = :updatedAt
                            WHERE sender_id = :senderId AND receiver_id = :receiverId
                        """)
                .param("senderId", contactRequest.getSenderId())
                .param("receiverId", contactRequest.getReceiverId())
                .param("status", contactRequest.getStatus().name())
                .param("updatedAt", Timestamp.from(Instant.now()))
                .update();
    }

    List<ContactRequest> findContactRequestBetweenUsers(Long senderId, Long receiverId) {
        return this.jdbcClient.sql("""
                            SELECT cr.status FROM contact_requests cr
                            WHERE (sender_id = :senderId AND receiver_id = :receiverId) OR (sender_id = :receiverId AND receiver_id = :senderId)
                            ORDER BY cr.created_at DESC
                        """)
                .param("senderId", senderId)
                .param("receiverId", receiverId)
                .query(ContactRequest.class)
                .list();
    }

    Optional<ContactRequest> findPendingContactRequestBySenderAndReceiverId(Long senderId, Long receiverId) {
        return this.jdbcClient.sql("""
                            SELECT cr.sender_id, cr.receiver_id, cr.status FROM contact_requests cr
                            WHERE sender_id = :senderId AND receiver_id = :receiverId AND status = 'PENDING'
                        """)
                .param("senderId", senderId)
                .param("receiverId", receiverId)
                .query(ContactRequest.class)
                .optional();
    }

    // We are returning the information of the sender
    List<User> findPendingContactRequestsByReceiverId(Long receiverId) {
        return this.jdbcClient.sql("""
                            SELECT u.id, u.username
                            FROM users u
                            JOIN contact_requests cr ON u.id = cr.sender_id
                            WHERE cr.receiver_id = :receiverId AND status = 'PENDING'
                            ORDER BY cr.created_at DESC
                        """)
                .param("receiverId", receiverId)
                .query(User.class)
                .list();
    }
}
