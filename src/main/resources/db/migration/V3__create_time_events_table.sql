CREATE TABLE IF NOT EXISTS time_events (
    id uuid,
    user_id INTEGER NOT NULL,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    start_time_zone_id TEXT NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time_zone_id TEXT NOT NULL,
    -- It can not be null because if it was during parsing it is set to NEVER
    repetition_frequency repetition_frequency NOT NULL,
    repetition_step INTEGER NULL,
    monthly_repetition_type monthly_repetition_type NULL,
    repetition_duration repetition_duration NULL,
    repetition_end_date DATE NULL,
    repetition_count INTEGER NULL,
    CONSTRAINT pk_time_events PRIMARY KEY (id),
    CONSTRAINT fk_time_events_users_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS time_event_slots(
    id uuid,
    time_event_id uuid NOT NULL,
    name VARCHAR(50) NOT NULL,
    location VARCHAR(50) NULL,
    description TEXT NULL,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    start_time_zone_id TEXT NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time_zone_id TEXT NOT NULL,
    CONSTRAINT pk_time_event_slots PRIMARY KEY (id),
    CONSTRAINT fk_time_event_slots_time_events_id FOREIGN KEY (time_event_id) REFERENCES time_events ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS time_event_slot_guest_emails (
    time_event_slot_id uuid,
    email TEXT NOT NULL,
    CONSTRAINT pk_time_event_slot_guest_emails PRIMARY KEY (time_event_slot_id, email),
    CONSTRAINT fk_time_event_slot_guest_emails_time_event_slots_id FOREIGN key (time_event_slot_id) REFERENCES time_event_slots(id) ON DELETE CASCADE
);