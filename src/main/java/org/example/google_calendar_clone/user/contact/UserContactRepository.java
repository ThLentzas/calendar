package org.example.google_calendar_clone.user.contact;

import org.example.google_calendar_clone.entity.UserContact;
import org.example.google_calendar_clone.entity.key.UserContactId;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserContactRepository extends JpaRepository<UserContact, UserContactId> {
}
