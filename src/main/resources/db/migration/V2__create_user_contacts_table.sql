CREATE TYPE contact_request_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED'
);

/*
    java Instant to psql is TIMESTAMP WITH TIME ZONE
 */
CREATE TABLE IF NOT EXISTS contact_requests (
    sender_id INTEGER NOT NULL,
    receiver_id INTEGER NOT NULL,
    status contact_request_status NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_contact_requests PRIMARY KEY (sender_id, receiver_id),
    CONSTRAINT fk_contact_requests_users_sender_id FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contact_requests_users_receiver_id FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS contacts (
    user_id_1 INTEGER NOT NULL,
    user_id_2 INTEGER NOT NULL,
    CONSTRAINT chk_user_id_order CHECK (user_id_1 < user_id_2),
    CONSTRAINT pk_contacts PRIMARY KEY (user_id_1, user_id_2),
    CONSTRAINT fk_contacts_users_user_id_1 FOREIGN KEY (user_id_1) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contacts_users_user_id_2 FOREIGN KEY (user_id_2) REFERENCES users(id) ON DELETE CASCADE
);