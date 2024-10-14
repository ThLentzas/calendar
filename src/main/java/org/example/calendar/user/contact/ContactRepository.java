package org.example.calendar.user.contact;

import org.example.calendar.entity.Contact;
import org.example.calendar.entity.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ContactRepository {
    private final JdbcClient jdbcClient;

    /*
        We are inserting in the join table once the user accepted a pending contact request. We need to handle the case
        of inserting a duplicate PK. The PK in the join table is the composite key of (userId1, userId2). There are
        3 different approaches to handle it.
            1. ON CONFLICT DO NOTHING, this syntax is psql specific but same idea
            2. Perform a SELECT and execute the INSERT if no results (SELECT COUNT() etc)
            3. a try/catch block where we handle a DuplicateKeyException

        The below check is not necessary because if the contact already exists with the provided ids, a contact request
        record would exist with status ACCEPTED and the check we perform in the contact request service would result
        in a ConflictException.

        if (insert != 1) {
            LOGGER.info("Duplicate key: {}, {}", contact.getUser1Id(), contact.getUser2Id());
        }

        Also the "ON CONFLICT DO NOTHING" is not needed
     */
    void create(Contact contact) {
        this.jdbcClient.sql("""
                        INSERT INTO contacts
                        VALUES(:userId1, :userId2)
                        """)
                .param("userId1", contact.getUser1Id())
                .param("userId2", contact.getUser2Id())
                .update();
    }

    /*
        In contacts, the relationship is bidirectional with a single entry. It means that user 1 has user 3 in their
        contacts and vice versa.
                                    INSERT INTO contacts VALUES (1, 3);
        If we query for the user with id 1, we want to fetch user 3, (1, 2 we want to fetch user 2 etc). We also want to
        avoid doing the filtering at the application level. A JPQL query like this is not efficient.

            SELECT c
            FROM Contact c
            JOIN FETCH c.user1
            JOIN FETCH c.user2
            WHERE c.user1.id = :userId or c.user2.id = :userId

        At application level we would have to stream the contacts, filter the list for users only that don't match the id
        (matching the id it would mean we fetched the current user) and then map that list to UserProfile. Instead, we use
        CASE WHEN. JPA CASE only supports scalar_expressions, it does not support entity/path expressions.
        https://stackoverflow.com/questions/40833498/jpa-query-with-case-when-exists-does-not-work-with-hibernate

                An entity expression refers to a whole entity, like a User object, or a path that represents an entire
                entity or relationship. Path expressions refer to navigation through entity relationships, such as
                c.user1 or c.user2, where c.user1 refers to a whole User entity associated with c.

                SELECT
                    CASE
                        WHEN c.user1.id = :userId THEN c.user2
                        ELSE c.user1
                FROM Contact c
                WHERE c.user1.id = :userId OR c.user2.id = :userId

        We need to use scalar expressions like the one below, to return something like an integer, string, or boolean value
        We can not map the response to Hibernate Entity.

        The flow:
            1. WHERE clause filters the rows from the Contact table to include only those rows where either user1.id or
            user2.id matches the given userId
            2. SELECT clause, with the CASE statements, determines what values should be retrieved for each column
            (id, username, and email)
                if the user1.id matches the userId provided, the values from user2 are selected (e.g., user2.id, user2.username, user2.email).
                If not, the values from user1 are selected
            3. ORDER BY clause, needs to know which username was selected to order them correctly
     */
    List<User> findContacts(Long userId) {
        return this.jdbcClient.sql("""
                        SELECT
                            CASE
                                WHEN c.user_id_1 = :userId THEN u2.id
                                ELSE u1.id
                            END as id,
                            CASE
                                WHEN c.user_id_1 = :userId THEN u2.username
                                ELSE u1.username
                            END as username,
                            CASE
                                WHEN c.user_id_1 = :userId THEN u2.password
                                ELSE u1.password
                            END as password,
                            CASE
                                WHEN c.user_id_1 = :userId THEN u2.email
                                ELSE u1.email
                            END as email
                        FROM contacts c
                        JOIN users u1 ON c.user_id_1 = u1.id
                        JOIN users u2 ON c.user_id_2 = u2.id
                        WHERE c.user_id_1 = :userId OR c.user_id_2 = :userId
                        ORDER BY username
                        """)
                .param("userId", userId)
                .query(User.class)
                .list();
    }
}
