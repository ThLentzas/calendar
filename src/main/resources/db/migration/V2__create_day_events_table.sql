CREATE TYPE repetition_frequency AS ENUM (
    'NEVER',
    'DAILY',
    'WEEKLY',
    'MONTHLY',
    'ANNUALLY'
);

CREATE TYPE repetition_duration AS ENUM (
    'FOREVER',
    'N_REPETITIONS',
    'UNTIL_DATE'
);

CREATE TYPE monthly_repetition_type AS ENUM (
    'SAME_DAY',
    'SAME_WEEKDAY'
);

CREATE TABLE IF NOT EXISTS day_events (
    id uuid,
    user_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    -- It can not be null because if it was during parsing, it is set to NEVER
    repetition_frequency repetition_frequency NOT NULL,
    repetition_step INTEGER NULL,
    weekly_recurrence_days VARCHAR(56) NULL,
    monthly_repetition_type monthly_repetition_type NULL,
    repetition_duration repetition_duration NULL,
    repetition_end_date DATE NULL,
    repetition_occurrences INTEGER NULL,
    CONSTRAINT pk_day_events PRIMARY KEY (id),
    CONSTRAINT fk_day_events_users_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS day_event_slots(
    id uuid,
    day_event_id uuid NOT NULL,
    name VARCHAR(50) NOT NULL,
    location VARCHAR(50) NULL,
    description TEXT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    CONSTRAINT pk_day_event_slots PRIMARY KEY (id),
    CONSTRAINT fk_day_event_slots_day_events_id FOREIGN KEY (day_event_id) REFERENCES day_events ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS day_event_slot_guest_emails (
    day_event_slot_id uuid,
    email TEXT NOT NULL,
    CONSTRAINT pk_day_event_slot_guest_emails PRIMARY KEY (day_event_slot_id, email),
    CONSTRAINT fk_day_event_slot_guest_emails_day_event_slots_id FOREIGN key (day_event_slot_id) REFERENCES day_event_slots(id) ON DELETE CASCADE
);