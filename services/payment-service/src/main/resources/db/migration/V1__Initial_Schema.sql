-- TABLE: PAYMENTS
-- =========================

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    user_id VARCHAR(255) NOT NULL,

    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- TABLE: outbox_events
-- =========================
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload BYTEA NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_aggregate_id ON outbox_event(aggregate_id);

-- Index for searching by time (useful for cleanup jobs/troubleshooting)
CREATE INDEX idx_outbox_created_at ON outbox_event(created_at);