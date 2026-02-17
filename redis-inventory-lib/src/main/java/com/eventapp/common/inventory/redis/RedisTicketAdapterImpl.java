package com.eventapp.common.inventory.service.impl;

import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.common.inventory.dto.GhostReservationMetadata;
import com.eventapp.common.inventory.domain.EventLifecycleStatus;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.common.inventory.utils.EventRedisKeys;
import com.eventapp.sharedutils.exceptions.domain.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.CAPACITY_TOO_LOW;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.DUPLICATE_RESERVATION;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_ALREADY_CANCELED;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_NOT_FOUND_IN_REDIS;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_NOT_OPEN_FOR_RESALE;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_OPERATION_FAILED;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.INVALID_INVENTORY_STATE;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.INVENTORY_INTEGRITY_FAILURE;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.RESERVATION_EXPIRED;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.RESERVATION_QUANTITY_MISMATCH;

public class RedisTicketAdapterImpl implements ITicketCachePort {

    private final StringRedisTemplate redisTemplate;
    private final IInventoryTtlProvider ttlProvider;

    private final RedisScript<Long> initScript;
    private final RedisScript<Long> updateScript;
    private final RedisScript<Long> cancelScript;
    private final RedisScript<Long> deleteScript;
    private final RedisScript<Long> checkCapacityScript;
    private final RedisScript<Long> reserveScript;
    private final RedisScript<Long> confirmScript;
    private final RedisScript<Long> idempotentReleaseScript;
    private final RedisScript<Long> releaseScript;

    private static final Logger log = LoggerFactory.getLogger(RedisTicketAdapterImpl.class);


    public RedisTicketAdapterImpl(StringRedisTemplate redisTemplate,
                                  IInventoryTtlProvider ttlProvider,
                                  RedisScript<Long> initScript,
                                  RedisScript<Long> updateScript,
                                  RedisScript<Long> cancelScript,
                                  RedisScript<Long> deleteScript,
                                  RedisScript<Long> checkCapacityScript,
                                  RedisScript<Long> reserveScript,
                                  RedisScript<Long> confirmScript,
                                  RedisScript<Long> idempotentReleaseScript,
                                  RedisScript<Long> releaseScript) {
        this.redisTemplate = redisTemplate;
        this.ttlProvider = ttlProvider;
        this.initScript = initScript;
        this.updateScript = updateScript;
        this.cancelScript = cancelScript;
        this.deleteScript = deleteScript;
        this.checkCapacityScript = checkCapacityScript;
        this.reserveScript = reserveScript;
        this.confirmScript = confirmScript;
        this.idempotentReleaseScript = idempotentReleaseScript;
        this.releaseScript = releaseScript;
    }

    private static final String METADATA_DELIMITER = ":";

    private void handleResult(Long result, Long eventId) {
        if (result == null)
            throw new ValidationException(INVALID_INVENTORY_STATE, eventId);

        int resultCode = result.intValue();

        switch (resultCode) {
            case 1:
                return; // Success
            case -1:
                throw new ValidationException(EVENT_NOT_OPEN_FOR_RESALE);
            case -2:
                throw new ValidationException(CAPACITY_TOO_LOW);
            case -3:
                throw new ValidationException(INVENTORY_INTEGRITY_FAILURE, eventId);
            case -4:
                throw new ValidationException(RESERVATION_EXPIRED, eventId);
            case -5:
                throw new ValidationException(EVENT_NOT_FOUND_IN_REDIS, eventId);
            case -6:
                throw new ValidationException(RESERVATION_QUANTITY_MISMATCH);
            case -7:
                throw new ValidationException(EVENT_ALREADY_CANCELED);
            case -8:
                throw new ValidationException(DUPLICATE_RESERVATION, eventId);
            default:
                throw new ValidationException(EVENT_OPERATION_FAILED);
        }
    }

    @Override
    public void initializeInventory(Long eventId,
                                    int capacity,
                                    EventLifecycleStatus initialStatus) {
        Long result = redisTemplate.execute(
              initScript,
              List.of(
                    EventRedisKeys.capacityKey(eventId),
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId)
              ),
              String.valueOf(capacity),
              initialStatus.name()
        );

        handleResult(result, eventId);
    }

    @Override
    public void updateInventory(Long eventId,
                                int newCapacity,
                                EventLifecycleStatus cancelStatus) {
        Long result = redisTemplate.execute(
              updateScript,
              List.of(
                    EventRedisKeys.capacityKey(eventId),
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId)
              ),
              cancelStatus.name(),
              String.valueOf(newCapacity)
        );

        handleResult(result, eventId);
    }

    @Override
    public void cancelInventory(Long eventId,
                                EventLifecycleStatus cancelStatus) {
        Long result = redisTemplate.execute(
              cancelScript,
              List.of(
                    EventRedisKeys.capacityKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId)
              ),
              cancelStatus.name()
        );

        handleResult(result, eventId);

        log.info("Redis status updated to CANCELLED for event {}", eventId);
    }

    @Override
    public void deleteInventory(Long eventId) {
        Long result = redisTemplate.execute(
              deleteScript,
              List.of(
                    EventRedisKeys.capacityKey(eventId),
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId)
              )
        );

        handleResult(result, eventId);

        log.info("Redis inventory deleted for event {}", eventId);
    }

    @Override
    public void validateCapacityChange(Long eventId,
                                       int newCapacity,
                                       EventLifecycleStatus cancelStatus) {
        Long result = redisTemplate.execute(
              checkCapacityScript,
              List.of(
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId)
              ),
              String.valueOf(newCapacity),
              cancelStatus.name()
        );

        handleResult(result, eventId);
    }

    @Override
    public void reserveTicketWithLock(String userId,
                                      Long eventId,
                                      UUID bookingId,
                                      EventLifecycleStatus requiredStatus,
                                      int quantity) {
        long timerTtl = ttlProvider.getLockTtlSeconds();
        long shadowTtl = ttlProvider.getShadowTtlSeconds();

        String metadata = eventId + METADATA_DELIMITER + quantity;

        Long result = redisTemplate.execute(
              reserveScript,
              List.of(
                    EventRedisKeys.capacityKey(eventId),
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId),
                    EventRedisKeys.reservationLockKey(bookingId),
                    EventRedisKeys.reservationShadowKey(bookingId),
                    EventRedisKeys.userEventLockKey(userId, eventId)
              ),
              requiredStatus.name(),
              String.valueOf(quantity),
              String.valueOf(timerTtl),
              String.valueOf(shadowTtl),
              metadata
        );

        handleResult(result, eventId);
    }

    @Override
    public void confirmPurchase(String userId,
                                Long eventId,
                                UUID bookingId,
                                EventLifecycleStatus requiredStatus,
                                int quantity) {
        Long result = redisTemplate.execute(
              confirmScript,
              List.of(
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservationLockKey(bookingId),
                    EventRedisKeys.statusKey(eventId),
                    EventRedisKeys.reservationShadowKey(bookingId),
                    EventRedisKeys.userEventLockKey(userId, eventId)
              ),
              requiredStatus.name(),
              String.valueOf(quantity)
        );

        handleResult(result, eventId);

        log.info("Purchase confirmed for event {} (Booking ID: {}). Inventory adjusted from Reserved to Sold.", eventId, bookingId);
    }

    @Override
    public void releaseReservation(String userId,
                                   Long eventId,
                                   UUID bookingId,
                                   int quantity) {
        Long result = redisTemplate.execute(
              releaseScript,
              List.of(
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.reservationLockKey(bookingId),
                    EventRedisKeys.reservationShadowKey(bookingId),
                    EventRedisKeys.userEventLockKey(userId, eventId)
              ),
              String.valueOf(quantity) //The quantity to release
        );

        handleResult(result, eventId);

        log.info("Released {} reserved tickets for event {} (Booking ID: {})", quantity, eventId, bookingId);
    }

    @Override
    public void releaseReservedInventoryIdempotently(Long eventId,
                                                     UUID bookingId,
                                                     int quantity) {
        Long result = redisTemplate.execute(
              idempotentReleaseScript,
              List.of(
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.sentinelKey(bookingId)
              ),
              String.valueOf(quantity),
              String.valueOf(ttlProvider.getSentinelTtlSeconds())
        );

        handleResult(result, eventId);
    }

    @Override
    public boolean isReservationStillValid(UUID bookingId) {
        Boolean exists = redisTemplate.hasKey(EventRedisKeys.reservationLockKey(bookingId));
        return exists != null && exists;
    }

    @Override
    public void cleanUpMetadataKey(UUID bookingId) {
        // We only delete the Metadata (res:meta), NOT the Lock (res:lock).
        String key = EventRedisKeys.reservationShadowKey(bookingId);
        redisTemplate.unlink(key); //Non-blocking (async delete)
    }

    @Override
    public String getReservationMetadata(UUID bookingId) {
        String key = EventRedisKeys.reservationShadowKey(bookingId);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Optional<GhostReservationMetadata> getGhostReservationMetadata(UUID bookingId) {
        String metadata = this.getReservationMetadata(bookingId);

        if (metadata != null && metadata.contains(METADATA_DELIMITER)) {
            try {
                String[] parts = metadata.split(METADATA_DELIMITER);
                return Optional.of(new GhostReservationMetadata(
                      Long.parseLong(parts[0]),
                      Integer.parseInt(parts[1])
                ));
            } catch (Exception e) {
                log.error("Found malformed metadata for booking {}", bookingId);
            }
        }
        return Optional.empty();
    }
}