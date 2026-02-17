package com.eventapp.bookingservice.service.impl;

import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.request.InventoryReleaseRequest;
import com.eventapp.bookingservice.dto.response.BookingResponseDTO;
import com.eventapp.bookingservice.exceptions.domain.BusinessException;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.bookingservice.repository.BookingRepository;
import com.eventapp.bookingservice.service.IBookingService;
import com.eventapp.bookingservice.service.IOutboxInventoryReleasesService;
import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.common.inventory.domain.EventLifecycleStatus;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.sharedutils.security.ISecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.eventapp.bookingservice.enums.BookingStatus.PENDING;
import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.RESERVATION_INVALID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements IBookingService {
    private final BookingRepository bookingRepository;
    private final ISecurityService securityService;
    private final ITicketCachePort ticketCachePort;
    private final IOutboxInventoryReleasesService outboxService;
    private final IInventoryTtlProvider ttlProvider;

    private void executeSafeRelease(Long eventId,
                                    UUID bookingId,
                                    int quantity) {
        try {
            ticketCachePort.releaseReservedInventoryIdempotently(eventId, bookingId, quantity);
        } catch (Exception e) {
            log.warn("Redis release failed for {}. Saving to Outbox.", bookingId);
            outboxService.saveToOutbox(
                  new InventoryReleaseRequest(
                        bookingId,
                        eventId,
                        quantity
                  )
            );
        }
    }

    private void handlePotentialGhostReservation(UUID bookingId) {
        // Only proceed if the DB record is truly missing (Connection Loss scenario)
        if (!bookingRepository.existsById(bookingId)) {
            ticketCachePort.getGhostReservationMetadata(bookingId)
                  .ifPresent(meta -> {
                      // Execute the release (using the centralized logic)
                      executeSafeRelease(
                            meta.eventId(),
                            bookingId,
                            meta.quantity()
                      );

                      ticketCachePort.cleanUpMetadataKey(bookingId);
                  });
        }
    }

    @Transactional
    @Override
    public BookingResponseDTO reserveTickets(BookingRequestDTO request) {
        String currentUserId = securityService.getAuthenticatedUserId();
        UUID bookingId = UUID.randomUUID();

        // Atomically decrement available and increment reserved
        ticketCachePort.reserveTicketWithLock(
              currentUserId,
              request.eventId(),
              bookingId,
              EventLifecycleStatus.PUBLISHED,
              request.quantity()
        );

        Booking booking = new Booking()
              .setId(bookingId)
              .setEventId(request.eventId())
              .setQuantity(request.quantity())
              .setUserId(currentUserId);

        booking = bookingRepository.save(booking);
        log.info("Booking {} reserved for user {}.", bookingId, currentUserId);

        int uiBufferSeconds = 30;

        return new BookingResponseDTO(
              booking.getId(),
              booking.getStatus().name(),
              booking.getCreatedAt().plusSeconds(ttlProvider.getLockTtlSeconds() - uiBufferSeconds)
        );
    }

    @Transactional
    @Override
    public void cancelReservation(Long eventId, UUID bookingId) {
        String currentUserId = securityService.getAuthenticatedUserId();

        int rows = bookingRepository.cancelActiveBooking(
              bookingId,
              currentUserId,
              eventId,
              PENDING
        );

        if (rows <= 0)
            throw new BusinessException(RESERVATION_INVALID, eventId);

        int qty = bookingRepository.findQuantity(bookingId);

        ticketCachePort.releaseReservation(
              currentUserId,
              eventId,
              bookingId,
              qty
        );
    }

    @Transactional
    public void handleAutomaticExpiration(UUID bookingId) {
        // ATOMIC DB GUARD
        int rowsUpdated = bookingRepository.markAsExpired(bookingId);

        if (rowsUpdated > 0) {
            // Standard Expiry (DB is Source of Truth)
            bookingRepository.findById(bookingId)
                  .ifPresent(booking ->
                        executeSafeRelease(
                              booking.getEventId(),
                              booking.getId(),
                              booking.getQuantity()
                        )
                  );
        } else {
            // Ghost/Failed Reservation (Redis is Source of Truth)
            handlePotentialGhostReservation(bookingId);
        }
    }
}
