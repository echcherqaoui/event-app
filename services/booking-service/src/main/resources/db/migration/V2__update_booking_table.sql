-- Update booking table timestamps
ALTER TABLE booking
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- Add new columns
ALTER TABLE booking
    ADD COLUMN user_email VARCHAR(255),
    ADD COLUMN price NUMERIC(19,2) NOT NULL DEFAULT 0;
