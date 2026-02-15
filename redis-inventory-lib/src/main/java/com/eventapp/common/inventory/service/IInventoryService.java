package com.eventapp.common.inventory.service;

import com.eventapp.common.inventory.dto.GhostReservationMetadata;
import com.eventapp.common.inventory.enums.EventLifecycleStatus;

import java.util.Optional;
import java.util.UUID;

public interface IInventoryService {
    void initializeInventory(Long eventId,
                             int capacity,
                             EventLifecycleStatus status);

    void updateInventory(Long eventId,
                         int newCapacity,
                         EventLifecycleStatus status);

    void cancelInventory(Long eventId,
                         EventLifecycleStatus status);

    void deleteInventory(Long eventId);

    void validateCapacityChange(Long eventId,
                                int newCapacity,
                                EventLifecycleStatus cancelStatus);

    void reserveTicketWithLock(String userId,
                               Long eventId,
                               UUID bookingId,
                               EventLifecycleStatus permittedStatus,
                               int quantity);

    void confirmPurchase(String userId,
                         Long eventId,
                         UUID bookingId,
                         EventLifecycleStatus requiredStatus,
                         int quantity);

    void releaseReservation(String userId,
                            Long eventId,
                            UUID bookingId,
                            int quantity);

    void releaseReservedInventoryIdempotently(Long eventId,
                                              UUID bookingId,
                                              int quantity);

    boolean isReservationStillValid(UUID bookingId);

    String getReservationMetadata(UUID bookingId);

    void cleanUpMetadataKey(UUID bookingId);

    Optional<GhostReservationMetadata> getGhostReservationMetadata(UUID bookingId);
}
