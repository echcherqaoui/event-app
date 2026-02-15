package com.eventapp.common.inventory.service.impl;

import com.eventapp.common.inventory.config.IInventoryTtlProvider;
import com.eventapp.common.inventory.dto.GhostReservationMetadata;
import com.eventapp.common.inventory.enums.EventLifecycleStatus;
import com.eventapp.common.inventory.service.IInventoryService;
import com.eventapp.common.inventory.utils.EventRedisKeys;
import com.eventapp.sharedutils.exceptions.domain.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements IInventoryService {

    private final StringRedisTemplate redisTemplate;
    private final IInventoryTtlProvider ttlProvider;

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

    // KEYS[1]: capacity, KEYS[2]: sold, KEYS[3]: reserved, KEYS[4]: status
    // ARGV[1]: capacity, ARGV[2]: status
    private static final DefaultRedisScript<Void> INIT_SCRIPT =
          new DefaultRedisScript<>("""
                  -- Do nothing if already exists
                  if redis.call('EXISTS', KEYS[1]) == 1 then return end
                
                  redis.call('SET', KEYS[1], ARGV[1])
                  redis.call('SET', KEYS[2], '0')
                  redis.call('SET', KEYS[3], '0')
                  redis.call('SET', KEYS[4], ARGV[2])
                """, Void.class);

    @Override
    public void initializeInventory(Long eventId,
                                    int capacity,
                                    EventLifecycleStatus initialStatus) {
        redisTemplate.execute(
              INIT_SCRIPT,
              List.of(
                    EventRedisKeys.capacityKey(eventId),
                    EventRedisKeys.soldKey(eventId),
                    EventRedisKeys.reservedKey(eventId),
                    EventRedisKeys.statusKey(eventId)
              ),
              String.valueOf(capacity),
              initialStatus.name()
        );
    }

    // KEYS[1]: capacity, KEYS[2]: sold, KEYS[3]: reserved, KEYS[4]=status
    // ARGV[1]: blockStatus, ARGV[2]: newCapacity,
    private static final DefaultRedisScript<Long> UPDATE_CAPACITY_SCRIPT =
          new DefaultRedisScript<>("""
                  if redis.call('EXISTS', KEYS[1]) == 0 then return -5 end
                
                  local status = redis.call('GET', KEYS[4])
                
                  if status == ARGV[1] then return -7 end
                
                  local newCapacity = tonumber(ARGV[2])
                  local sold = tonumber(redis.call('GET', KEYS[2]) or "0")
                  local reserved = tonumber(redis.call('GET', KEYS[3]) or "0")
                
                  if newCapacity < (sold + reserved) then return -2 end
                
                  redis.call('SET', KEYS[1], newCapacity)
                  return 1
                """, Long.class);

    @Override
    public void updateInventory(Long eventId,
                                int newCapacity,
                                EventLifecycleStatus cancelStatus) {
        Long result = redisTemplate.execute(
              UPDATE_CAPACITY_SCRIPT,
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

    // KEYS[1]: capacity, KEYS[2]: reserved, KEYS[3]: status,
    // ARGV[1]: blockedStatus
    private static final DefaultRedisScript<Long> CANCEL_SCRIPT =
          new DefaultRedisScript<>("""
                  -- Ensure event actually exists in inventory
                  if redis.call('EXISTS', KEYS[1]) == 0 then return -5 end
                
                  -- Check if already cancelled to avoid redundant updates
                  local currentStatus = redis.call('GET', KEYS[3])
                  if currentStatus == ARGV[1] then return -7 end
                
                  redis.call('SET', KEYS[3], ARGV[1])
                  redis.call('SET', KEYS[2], '0')
                  return 1
                """, Long.class);

    @Override
    public void cancelInventory(Long eventId,
                                EventLifecycleStatus cancelStatus) {
        Long result = redisTemplate.execute(
              CANCEL_SCRIPT,
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

    // KEYS[1]: capacity, KEYS[2]: sold, KEYS[3]: reserved, KEYS[4]: status
    private static final DefaultRedisScript<Long> DELETE_SCRIPT =
          new DefaultRedisScript<>("""
                  local sold = tonumber(redis.call('GET', KEYS[2]) or "0")
                  local reserved = tonumber(redis.call('GET', KEYS[3]) or "0")
                
                  -- Indicate failure/cancellation required instead
                  if (sold + reserved) > 0 then return -2 end
                
                  -- Delete all inventory associated keys
                  redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4])
                  return 1
                """, Long.class);

    @Override
    public void deleteInventory(Long eventId) {
        Long result = redisTemplate.execute(
              DELETE_SCRIPT,
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

    // KEYS[1]: sold, KEYS[2]: reserved,  KEYS[3]: status
    // ARGV[1]: newCapacity, ARGV[2]: blockStatus
    private static final DefaultRedisScript<Long> CHECK_CAPACITY_SCRIPT =
          new DefaultRedisScript<>("""
                  local status = redis.call('GET', KEYS[3])
                  if status == ARGV[2] then return -7 end
                
                  local sold = tonumber(redis.call('GET', KEYS[1]) or '0')
                  local reserved = tonumber(redis.call('GET', KEYS[2]) or '0')
                  local newCapacity = tonumber(ARGV[1])
                
                  if newCapacity < (sold + reserved) then return -2 end
                
                  return 1
                """, Long.class);

    @Override
    public void validateCapacityChange(Long eventId,
                                       int newCapacity,
                                       EventLifecycleStatus cancelStatus) {
        Long result = redisTemplate.execute(
              CHECK_CAPACITY_SCRIPT,
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

    // KEYS[1]: capacity, [2]: sold, [3]: reserved, [4]: status
    // KEYS[5]: timerKey ("res:lock:UUID") -> The expiration trigger
    // KEYS[6]: dataKey ("res:meta:UUID")  -> The forensic metadata backup
    // KEYS[7]: userLockKey ("u:l:UID:EID")
    // ARGV[1]: requiredStatus, [2]: requestedQty, [3]: timerTtl, [4]: dataTtl, [5]: metadata
    private static final DefaultRedisScript<Long> RESERVE_WITH_LOCK_SCRIPT =
          new DefaultRedisScript<>("""
                  -- Anti-Hoarding Check (User already has an active reservation)
                  if redis.call('EXISTS', KEYS[7]) == 1 then return -8 end
                
                  local status = redis.call('GET', KEYS[4])
                
                  -- Not eligible for reservations
                  if status ~= ARGV[1] then return -1 end
                
                  local capacity = tonumber(redis.call('GET', KEYS[1]) or '0')
                  local sold = tonumber(redis.call('GET', KEYS[2]) or '0')
                  local globalReserved = tonumber(redis.call('GET', KEYS[3]) or '0')
                  local requested = tonumber(ARGV[2])
                  local ttl = tonumber(ARGV[3]);
                
                  -- Insufficient inventory
                  if capacity - (sold + globalReserved) < requested then return -2 end
                
                  redis.call('INCRBY', KEYS[3], requested)
                
                  -- Set Expiration Trigger
                  redis.call('SET', KEYS[5], requested, 'EX', ttl)
                
                  -- Set Shadow Metadata: Lives longer to allow recovery if DB save fails
                  redis.call('SET', KEYS[6], ARGV[5], 'EX', tonumber(ARGV[4]))
                
                  redis.call('SET', KEYS[7], '1', 'EX', ttl) -- Lock the user
                  return 1
                """, Long.class);

    @Override
    public void reserveTicketWithLock(String userId,
                                      Long eventId,
                                      UUID bookingId,
                                      EventLifecycleStatus requiredStatus,
                                      int quantity) {
        long timerTtl = ttlProvider.getLockTtlSeconds();
        long shadowTtl = ttlProvider.getShadowTtlSeconds();

        String metadata = eventId + ":" + quantity;

        Long result = redisTemplate.execute(
              RESERVE_WITH_LOCK_SCRIPT,
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

    // KEYS[1]: reserved, KEYS[2]: sold, KEYS[3]: lock,  KEYS[4]: status
    // KEYS[5]: dataKey, KEYS[6]: userLockKey
    // ARGV[1]: status, ARGV[2]: to_release
    private static final DefaultRedisScript<Long> CONFIRM_SCRIPT =
          new DefaultRedisScript<>("""
                  local status = redis.call('GET', KEYS[4])
                  -- Not eligible for confirmation
                  if status ~= ARGV[1] then return -1 end

                  local reservedQuantity = tonumber(redis.call('GET', KEYS[3]) or '0')
                
                  -- Reservation expired (Shadow Key is gone)
                  if reservedQuantity == 0 then return -4 end
                
                  local to_release = tonumber(ARGV[2])
                
                  -- Mismatch (User trying to pay for fewer/more than reserved)
                  if reservedQuantity ~= to_release then return -6 end
                
                  local globalReserved = tonumber(redis.call('GET', KEYS[1]) or '0')
                
                  -- Data inconsistency (Global reserved count too low)
                  if globalReserved < reservedQuantity then return -3 end
                
                  redis.call('DECRBY', KEYS[1], reservedQuantity)
                  redis.call('INCRBY', KEYS[2], reservedQuantity)
                
                  -- Atomic Cleanup of ALL keys
                  redis.call('DEL', KEYS[3], KEYS[5], KEYS[6])
                  return 1
                """, Long.class);

    @Override
    public void confirmPurchase(String userId,
                                Long eventId,
                                UUID bookingId,
                                EventLifecycleStatus requiredStatus,
                                int quantity) {
        Long result = redisTemplate.execute(
              CONFIRM_SCRIPT,
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

    // KEYS[1]: reserved, KEYS[2]: lock, KEYS[3]: meta, KEYS[4]: userLock
    // ARGV[1]: quantity
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
          new DefaultRedisScript<>("""
                  -- Already expired or released
                  if redis.call('EXISTS', KEYS[3]) == 0 then return 1 end
                
                  local globalReserved = tonumber(redis.call('GET', KEYS[1]) or '0')
                  local to_release = tonumber(ARGV[1])
                
                  -- Data inconsistency
                  if globalReserved < to_release then return -3 end
                
                  redis.call('DECRBY', KEYS[1], to_release)
                
                  -- Atomic Cleanup of all related keys
                  redis.call('DEL', KEYS[2], KEYS[3], KEYS[4])
                  return 1
                """, Long.class);

    @Override
    public void releaseReservation(String userId,
                                   Long eventId,
                                   UUID bookingId,
                                   int quantity) {
        Long result = redisTemplate.execute(
              RELEASE_SCRIPT,
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

    // KEYS[1]: reserved, KEYS[2]: sentinel_key
    // ARGV[1]: quantity, ARGV[2]: sentinel_ttl
    private static final DefaultRedisScript<Long> IDEMPOTENT_RELEASE_SCRIPT =
          new DefaultRedisScript<>("""
                   -- Already processed
                  if redis.call('EXISTS', KEYS[2]) == 1 then return 1 end
                
                  local globalReserved = tonumber(redis.call('GET', KEYS[1]) or '0')
                  local to_release = tonumber(ARGV[1])
                
                  -- Data inconsistency
                  if globalReserved < to_release then return -3  end
                
                  redis.call('DECRBY', KEYS[1], to_release)
                  redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[2]))
                  return 1
                """, Long.class);

    @Override
    public void releaseReservedInventoryIdempotently(Long eventId,
                                                     UUID bookingId,
                                                     int quantity) {
        Long result = redisTemplate.execute(
              IDEMPOTENT_RELEASE_SCRIPT,
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

        if (metadata != null && metadata.contains(":")) {
            try {
                String[] parts = metadata.split(":");
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