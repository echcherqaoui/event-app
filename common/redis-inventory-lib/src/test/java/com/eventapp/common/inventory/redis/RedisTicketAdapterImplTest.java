package com.eventapp.common.inventory.redis;

import com.eventapp.common.inventory.dto.GhostReservationMetadata;
import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.sharedutils.exceptions.domain.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.eventapp.common.inventory.domain.EventLifecycleStatus.CANCELLED;
import static com.eventapp.common.inventory.domain.EventLifecycleStatus.PUBLISHED;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.CAPACITY_TOO_LOW;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.DUPLICATE_RESERVATION;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_ALREADY_CANCELED;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_NOT_FOUND_IN_REDIS;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.EVENT_NOT_OPEN_FOR_RESALE;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.INVALID_INVENTORY_STATE;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.INVENTORY_INTEGRITY_FAILURE;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.RESERVATION_EXPIRED;
import static com.eventapp.common.inventory.exceptions.InventoryErrorCode.RESERVATION_QUANTITY_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTicketAdapterImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private IInventoryTtlProvider ttlProvider;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RedisScript<Long> initScript,
          updateScript,
          cancelScript,
          deleteScript,
          checkCapacityScript,
          reserveScript,
          confirmScript,
          idempotentReleaseScript,
          releaseScript;

    @Captor
    private ArgumentCaptor<List<String>> keysCaptor;

    private RedisTicketAdapterImpl adapter;

    private static final Long EVENT_ID = 1L;
    private static final int CAPACITY = 100;
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        adapter = new RedisTicketAdapterImpl(
              redisTemplate,
              ttlProvider,
              initScript,
              updateScript,
              cancelScript,
              deleteScript,
              checkCapacityScript,
              reserveScript,
              confirmScript,
              idempotentReleaseScript,
              releaseScript
        );
    }

    private void assertHandleResultMapping(org.assertj.core.api.ThrowableAssert.ThrowingCallable methodCall) {
        var errorScenarios = java.util.Map.of(
              -1L, EVENT_NOT_OPEN_FOR_RESALE,
              -2L, CAPACITY_TOO_LOW,
              -3L, INVENTORY_INTEGRITY_FAILURE,
              -4L, RESERVATION_EXPIRED,
              -5L, EVENT_NOT_FOUND_IN_REDIS,
              -6L, RESERVATION_QUANTITY_MISMATCH,
              -7L, EVENT_ALREADY_CANCELED,
              -8L, DUPLICATE_RESERVATION
        );

        errorScenarios.forEach((resultCode, expectedError) -> {
            reset(redisTemplate);
            // Use specific matchers to help Mockito find the right overloaded method

            when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                  .thenReturn(resultCode);

            assertThatThrownBy(methodCall)
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", expectedError);
        });
    }

    @Nested
    @DisplayName("Initialize Inventory")
    class InitializeInventory {
        @Test
        @DisplayName("Should successfully initialize and verify exact Redis key order")
        void shouldInitialize_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(initScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.initializeInventory(EVENT_ID, CAPACITY, PUBLISHED);

            verify(redisTemplate).execute(eq(initScript), keysCaptor.capture(), eq("100"), eq("PUBLISHED"));

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.capacityKey(EVENT_ID),
                  EventRedisKeys.soldKey(EVENT_ID),
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.statusKey(EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should map all possible result codes from handleResult")
        void shouldMapAllErrors_ThroughHandleResult() {
            assertHandleResultMapping(() ->
                  adapter.initializeInventory(EVENT_ID, CAPACITY, PUBLISHED));
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                  .thenReturn(null);

            assertThatThrownBy(() -> adapter.initializeInventory(EVENT_ID, CAPACITY, PUBLISHED))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Update Inventory")
    class UpdateInventory {
        private static final int NEW_CAPACITY = 200;

        @Test
        @DisplayName("Should successfully update and verify key/argument order")
        void shouldUpdate_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(updateScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.updateInventory(EVENT_ID, NEW_CAPACITY, CANCELLED);

            verify(redisTemplate).execute(
                  eq(updateScript),
                  keysCaptor.capture(),
                  eq("CANCELLED"),
                  eq("200")
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.capacityKey(EVENT_ID),
                  EventRedisKeys.soldKey(EVENT_ID),
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.statusKey(EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should map all possible result codes from handleResult")
        void shouldMapAllErrors_ThroughHandleResult() {
            assertHandleResultMapping(() ->
                  adapter.updateInventory(EVENT_ID, NEW_CAPACITY, CANCELLED));
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                  .thenReturn(null);

            assertThatThrownBy(() -> adapter.updateInventory(EVENT_ID, NEW_CAPACITY, CANCELLED))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Cancel Inventory")
    class CancelInventory {

        @Test
        @DisplayName("Should successfully cancel and verify specific 3-key set")
        void shouldCancel_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(cancelScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.cancelInventory(EVENT_ID, CANCELLED);

            verify(redisTemplate).execute(
                  eq(cancelScript),
                  keysCaptor.capture(),
                  eq("CANCELLED")
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.capacityKey(EVENT_ID),
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.statusKey(EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should map all possible result codes for cancellation")
        void shouldMapAllErrors_ThroughHandleResult() {
            assertHandleResultMapping(() ->
                  adapter.cancelInventory(EVENT_ID, CANCELLED));
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                  .thenReturn(null);

            assertThatThrownBy(() -> adapter.cancelInventory(EVENT_ID, CANCELLED))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Delete Inventory")
    class DeleteInventory {

        @Test
        @DisplayName("Should successfully delete and verify no additional arguments are passed")
        void shouldDelete_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(deleteScript), anyList()))
                  .thenReturn(1L);

            adapter.deleteInventory(EVENT_ID);

            verify(redisTemplate).execute(eq(deleteScript), keysCaptor.capture());

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.capacityKey(EVENT_ID),
                  EventRedisKeys.soldKey(EVENT_ID),
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.statusKey(EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(eq(deleteScript), anyList()))
                  .thenReturn(null);

            assertThatThrownBy(() -> adapter.deleteInventory(EVENT_ID))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Validate Capacity Change")
    class ValidateCapacityChange {

        private static final int VALID_CAPACITY = 150;

        @Test
        @DisplayName("Should successfully validate and verify 3-key set and ARGV order")
        void shouldValidate_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(checkCapacityScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.validateCapacityChange(EVENT_ID, VALID_CAPACITY, PUBLISHED);

            verify(redisTemplate).execute(
                  eq(checkCapacityScript),
                  keysCaptor.capture(),
                  eq("150"),
                  eq("PUBLISHED")
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.soldKey(EVENT_ID),
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.statusKey(EVENT_ID)
            );
        }


        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                  .thenReturn(null);

            assertThatThrownBy(() ->
                  adapter.validateCapacityChange(EVENT_ID, VALID_CAPACITY, PUBLISHED))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Reserve Ticket With Lock")
    class ReserveTicketWithLock {
        private static final int QUANTITY = 2;
        private static final long LOCK_TTL = 300L;
        private static final long SHADOW_TTL = 3600L;

        @Test
        @DisplayName("Should successfully reserve and verify 7-key set and metadata string")
        void shouldReserve_WhenRedisReturnsSuccess() {
            when(ttlProvider.getLockTtlSeconds()).thenReturn(LOCK_TTL);
            when(ttlProvider.getShadowTtlSeconds()).thenReturn(SHADOW_TTL);
            when(redisTemplate.execute(eq(reserveScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.reserveTicketWithLock(USER_ID, EVENT_ID, BOOKING_ID, PUBLISHED, QUANTITY);


            // Expected Metadata: "1:2" (eventId:quantity)
            String expectedMetadata = EVENT_ID + ":" + QUANTITY;

            verify(redisTemplate).execute(
                  eq(reserveScript),
                  keysCaptor.capture(),
                  eq("PUBLISHED"),
                  eq("2"),
                  eq("300"),
                  eq("3600"),
                  eq(expectedMetadata)
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.capacityKey(EVENT_ID),
                  EventRedisKeys.soldKey(EVENT_ID),
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.statusKey(EVENT_ID),
                  EventRedisKeys.reservationLockKey(BOOKING_ID),
                  EventRedisKeys.reservationShadowKey(BOOKING_ID),
                  EventRedisKeys.userEventLockKey(USER_ID, EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should map all possible result codes for reservation")
        void shouldMapAllErrors_ThroughHandleResult() {
            when(ttlProvider.getLockTtlSeconds()).thenReturn(LOCK_TTL);
            when(ttlProvider.getShadowTtlSeconds()).thenReturn(SHADOW_TTL);

            assertHandleResultMapping(() ->
                  adapter.reserveTicketWithLock(USER_ID, EVENT_ID, BOOKING_ID, PUBLISHED, QUANTITY));
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(ttlProvider.getLockTtlSeconds()).thenReturn(LOCK_TTL);
            when(ttlProvider.getShadowTtlSeconds()).thenReturn(SHADOW_TTL);
            when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(null);

            assertThatThrownBy(() ->
                  adapter.reserveTicketWithLock(USER_ID, EVENT_ID, BOOKING_ID, PUBLISHED, QUANTITY))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Confirm Purchase")
    class ConfirmPurchase {
        private static final int QUANTITY = 3;

        @Test
        @DisplayName("Should successfully confirm and verify 6-key set and ARGV order")
        void shouldConfirm_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(confirmScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.confirmPurchase(USER_ID, EVENT_ID, BOOKING_ID, PUBLISHED, QUANTITY);

            verify(redisTemplate).execute(
                  eq(confirmScript),
                  keysCaptor.capture(),
                  eq("PUBLISHED"),
                  eq("3")
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.soldKey(EVENT_ID),
                  EventRedisKeys.reservationLockKey(BOOKING_ID),
                  EventRedisKeys.statusKey(EVENT_ID),
                  EventRedisKeys.reservationShadowKey(BOOKING_ID),
                  EventRedisKeys.userEventLockKey(USER_ID, EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should map all possible result codes for confirmation")
        void shouldMapAllErrors_ThroughHandleResult() {
            assertHandleResultMapping(() ->
                  adapter.confirmPurchase(USER_ID, EVENT_ID, BOOKING_ID, PUBLISHED, QUANTITY));
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(null);

            assertThatThrownBy(() ->
                  adapter.confirmPurchase(USER_ID, EVENT_ID, BOOKING_ID, PUBLISHED, QUANTITY))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Release Reservation")
    class ReleaseReservation {
        private static final int QUANTITY_TO_RELEASE = 5;

        @Test
        @DisplayName("Should successfully release and verify 4-key set and quantity argument")
        void shouldRelease_WhenRedisReturnsSuccess() {
            when(redisTemplate.execute(eq(releaseScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.releaseReservation(USER_ID, EVENT_ID, BOOKING_ID, QUANTITY_TO_RELEASE);

            verify(redisTemplate).execute(
                  eq(releaseScript),
                  keysCaptor.capture(),
                  eq("5")
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.reservationLockKey(BOOKING_ID),
                  EventRedisKeys.reservationShadowKey(BOOKING_ID),
                  EventRedisKeys.userEventLockKey(USER_ID, EVENT_ID)
            );
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(null);

            assertThatThrownBy(() ->
                  adapter.releaseReservation(USER_ID, EVENT_ID, BOOKING_ID, QUANTITY_TO_RELEASE))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Idempotent Release")
    class IdempotentRelease {

        private static final int RELEASE_QUANTITY = 2;
        private static final long SENTINEL_TTL = 86400L; // 24 hours

        @Test
        @DisplayName("Should successfully release idempotently and verify 2-key set and sentinel TTL")
        void shouldReleaseIdempotently_WhenRedisReturnsSuccess() {
            when(ttlProvider.getSentinelTtlSeconds()).thenReturn(SENTINEL_TTL);
            when(redisTemplate.execute(eq(idempotentReleaseScript), anyList(), any(Object[].class)))
                  .thenReturn(1L);

            adapter.releaseReservedInventoryIdempotently(EVENT_ID, BOOKING_ID, RELEASE_QUANTITY);

            verify(redisTemplate).execute(
                  eq(idempotentReleaseScript),
                  keysCaptor.capture(),
                  eq("2"),
                  eq("86400")
            );

            assertThat(keysCaptor.getValue()).containsExactly(
                  EventRedisKeys.reservedKey(EVENT_ID),
                  EventRedisKeys.sentinelKey(BOOKING_ID)
            );
        }

        @Test
        @DisplayName("Should map all possible result codes for idempotent release")
        void shouldMapAllErrors_ThroughHandleResult() {
            when(ttlProvider.getSentinelTtlSeconds())
                  .thenReturn(SENTINEL_TTL);

            assertHandleResultMapping(() ->
                  adapter.releaseReservedInventoryIdempotently(EVENT_ID, BOOKING_ID, RELEASE_QUANTITY));
        }

        @Test
        @DisplayName("Should throw INVALID_INVENTORY_STATE when Redis result is null")
        void shouldThrowException_WhenResultIsNull() {
            when(ttlProvider.getSentinelTtlSeconds()).
                  thenReturn(SENTINEL_TTL);

            when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                  .thenReturn(null);

            assertThatThrownBy(() ->
                  adapter.releaseReservedInventoryIdempotently(EVENT_ID, BOOKING_ID, RELEASE_QUANTITY))
                  .isInstanceOf(ValidationException.class)
                  .hasFieldOrPropertyWithValue("errorCode", INVALID_INVENTORY_STATE);
        }
    }

    @Nested
    @DisplayName("Is Reservation Still Valid")
    class IsReservationStillValid {

        @Test
        @DisplayName("Should return true when key exists in Redis")
        void shouldReturnTrue_WhenKeyExists() {
            String key = EventRedisKeys.reservationLockKey(BOOKING_ID);
            when(redisTemplate.hasKey(key))
                  .thenReturn(true);

            boolean result = adapter.isReservationStillValid(BOOKING_ID);

            assertThat(result).isTrue();
            verify(redisTemplate).hasKey(key);
        }

        @Test
        @DisplayName("Should return false when key does not exist")
        void shouldReturnFalse_WhenKeyDoesNotExist() {
            when(redisTemplate.hasKey(anyString()))
                  .thenReturn(false);

            boolean result = adapter.isReservationStillValid(BOOKING_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false and handle null safely when Redis returns null")
        void shouldReturnFalse_WhenRedisReturnsNull() {
            // RedisTemplate can return null if the connection fails or operation times out
            when(redisTemplate.hasKey(anyString()))
                  .thenReturn(null);

            boolean result = adapter.isReservationStillValid(BOOKING_ID);

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("Should unlink only the shadow metadata key")
    void shouldCleanUpMetadataKey() {
        String expectedKey = EventRedisKeys.reservationShadowKey(BOOKING_ID);

        adapter.cleanUpMetadataKey(BOOKING_ID);

        verify(redisTemplate).unlink(expectedKey);
    }

    @Nested
    @DisplayName("Get Ghost Reservation Metadata")
    class GetGhostReservationMetadata {

        @Test
        @DisplayName("Should parse valid metadata string into DTO")
        void shouldReturnPopulatedOptional_WhenMetadataIsValid() {
            // Metadata format is "eventId:quantity"
            String validMetadata = "1:5";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(validMetadata);

            Optional<GhostReservationMetadata> result = adapter.getGhostReservationMetadata(BOOKING_ID);

            assertThat(result).isPresent();
            assertThat(result.get().eventId()).isEqualTo(1L);
            assertThat(result.get().quantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return empty optional when metadata is malformed or null")
        void shouldReturnEmptyOptional_WhenMetadataIsInvalid() {
            // Test with a malformed string
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            //  Malformed string
            when(valueOperations.get(anyString())).thenReturn("invalid-data");
            assertThat(adapter.getGhostReservationMetadata(BOOKING_ID)).isEmpty();

            // Null result from Redis
            when(valueOperations.get(anyString())).thenReturn(null);
            assertThat(adapter.getGhostReservationMetadata(BOOKING_ID)).isEmpty();
        }
    }
}