CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    -- SERIAL = java's integer or Integer(32-bit), BIGSERIAL(64-bit) = java's Long or long
    -- https://www.postgresql.org/docs/current/datatype-numeric.html
    id BIGSERIAL,
    email TEXT NOT NULL,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT unique_users_email UNIQUE (email)
);
