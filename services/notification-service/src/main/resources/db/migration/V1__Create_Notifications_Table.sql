CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    -- TECHNICAL ID: The Outbox Row ID (Idempotency Key)
    message_id VARCHAR(255) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    booking_id VARCHAR(255) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uc_notification_message_id UNIQUE (message_id)
);

CREATE INDEX idx_notifications_event_id ON notification(event_id);
CREATE INDEX idx_notifications_booking_id ON notification(booking_id);