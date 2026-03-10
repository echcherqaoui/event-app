-- TABLE: BOOKING
-- =========================

CREATE TABLE booking (
     id UUID PRIMARY KEY,

     event_id BIGINT NOT NULL,
     user_id VARCHAR(255) NOT NULL,

     quantity INTEGER NOT NULL CHECK (quantity >= 1 AND quantity <= 4),
     status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED',

     created_at TIMESTAMP NOT NULL,
     updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_booking_event_id ON booking(event_id);
CREATE INDEX idx_booking_user_id ON booking(user_id);
CREATE INDEX idx_booking_status ON booking(status);

-- TABLE: OUTBOX INVENTORY RELEASE
-- =========================

CREATE TABLE outbox_inventory_release (
      id BIGSERIAL PRIMARY KEY,

      booking_id UUID NOT NULL,
      event_id BIGINT NOT NULL,
      quantity INTEGER NOT NULL,

      retry_count INTEGER NOT NULL DEFAULT 0,
      processed BOOLEAN NOT NULL DEFAULT FALSE,

      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP
);

-- Matches your @Table index definition
CREATE INDEX idx_outbox_inventory_release
    ON outbox_inventory_release (processed, created_at, retry_count);
