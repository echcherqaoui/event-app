-- TABLE: EVENT
-- =========================

CREATE TABLE event (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    location VARCHAR(255) NOT NULL,
    organizer_id VARCHAR(255) NOT NULL,

    price NUMERIC(19,2) NOT NULL CHECK (price >= 0),
    capacity INTEGER NOT NULL CHECK (capacity >= 1),
    sold_count INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED',

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_event_status ON event(status);
CREATE INDEX idx_event_organizer_id ON event(organizer_id);
