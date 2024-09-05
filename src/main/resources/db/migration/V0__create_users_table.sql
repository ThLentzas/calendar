CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id SERIAL,
    email TEXT NOT NULL,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT unique_users_email UNIQUE (email)
);
