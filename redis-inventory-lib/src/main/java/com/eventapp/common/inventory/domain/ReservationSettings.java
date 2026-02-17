package com.eventapp.common.inventory.domain;

public record ReservationSettings(long lockMin,
                                  long shadowBufferMin,
                                  long sentinelMin) {
    public ReservationSettings {
        if (lockMin < 1)
            throw new IllegalArgumentException("Lock TTL must be at least 1 minute.");

        if (shadowBufferMin < 1)
            throw new IllegalArgumentException("Shadow buffer must be at least 1 minute beyond the Lock TTL.");

        if (sentinelMin < 30)
            throw new IllegalArgumentException("Sentinel TTL must be at least 30 minutes to ensure recovery idempotency.");
    }

    public long lockTtlSeconds() {
        return lockMin * 60L;
    }

    public long shadowTtlSeconds() {
        return (lockMin + shadowBufferMin) * 60L;
    }

    public long sentinelTtlSeconds() {
        return sentinelMin * 60L;
    }
}