CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload BYTEA NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_aggregate_id ON outbox_event (aggregate_id);
CREATE INDEX idx_outbox_created_at ON outbox_event (created_at);