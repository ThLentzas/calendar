package org.example.google_calendar_clone.user.contact;

import org.example.google_calendar_clone.entity.ContactRequest;
import org.example.google_calendar_clone.entity.key.ContactRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ContactRequestRepository extends JpaRepository<ContactRequest, ContactRequestId> {
    
    @Query("""
                SELECT c
                FROM ContactRequest c
                JOIN FETCH c.sender
                WHERE (c.sender.id = :senderId AND c.receiver.id = :receiverId) OR (c.sender.id = :receiverId AND c.receiver.id = :senderId)
            """)
    List<ContactRequest> findContactRequestBetweenUsers(@Param("senderId") Long senderId,
                                                        @Param("receiverId") Long receiverId);

    @Query("""
                SELECT c
                FROM ContactRequest c
                JOIN FETCH c.sender
                JOIN FETCH c.receiver
                WHERE c.sender.id = :senderId AND c.receiver.id = :receiverId AND c.status = :status
            """)
    Optional<ContactRequest> findPendingContactRequestBySenderAndReceiverId(@Param("senderId") Long senderId,
                                                                            @Param("receiverId") Long receiverId,
                                                                            @Param("status") ContactRequestStatus status);

    // Returns a list of all pending contact requests the current user received, an empty list if none found
    @Query("""
                SELECT c
                FROM ContactRequest c
                JOIN FETCH c.sender
                WHERE c.receiver.id = :receiverId AND c.status = 'PENDING'
                ORDER BY c.createdAt DESC
            """)
    List<ContactRequest> findPendingContactRequestsByReceiverId(@Param("receiverId") Long receiverId);
}