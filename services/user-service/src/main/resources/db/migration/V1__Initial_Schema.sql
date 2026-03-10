-- TABLE: USERS
-- =========================

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),

    profile_complete BOOLEAN DEFAULT FALSE,
    phone_number VARCHAR(255),
    gender VARCHAR(255),
    bio VARCHAR(1000),
    birth_date DATE,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
