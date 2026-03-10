package com.eventapp.common.inventory.redis;

import java.util.UUID;

public final class EventRedisKeys {

    private EventRedisKeys() {}

    public static String capacityKey(Long eventId) {
        return "event:capacity:" + eventId;
    }

    public static String soldKey(Long eventId) {
        return "event:sold:" + eventId;
    }

    public static String reservedKey(Long eventId) {
        return "event:reserved:" + eventId;
    }

    public static String statusKey(Long eventId) {
        return "event:status:" + eventId;
    }

    public static String sentinelKey(UUID bookingId) {
        return "res:proc:" + bookingId;
    }

    public static String reservationLockKey(UUID bookingId) {
        return "res:lock:" + bookingId;
    }

    public static String reservationShadowKey(UUID bookingId) {
        return "res:meta:" + bookingId;
    }

    public static String userEventLockKey(String userId, Long eventId) {
        return "u:l:" + userId + ":" + eventId;
    }
}
