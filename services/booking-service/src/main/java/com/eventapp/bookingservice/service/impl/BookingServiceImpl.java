package com.eventapp.bookingservice.service.impl;

import com.eventapp.bookingservice.client.EventServiceGateway;
import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.request.InventoryReleaseRequest;
import com.eventapp.bookingservice.dto.response.BookingResponseDTO;
import com.eventapp.bookingservice.exceptions.domain.BookingNotFoundException;
import com.eventapp.bookingservice.mapper.BookingMapper;
import com.eventapp.bookingservice.outbox.BookingOutboxFactory;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.bookingservice.model.OutboxEvent;
import com.eventapp.bookingservice.repository.BookingRepository;
import com.eventapp.bookingservice.repository.OutboxEventRepository;
import com.eventapp.bookingservice.service.IBookingService;
import com.eventapp.bookingservice.service.IOutboxInventoryReleasesService;
import com.eventapp.common.inventory.domain.EventLifecycleStatus;
import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.sharedutils.exceptions.domain.BusinessException;
import com.eventapp.sharedutils.exceptions.domain.ForbiddenException;
import com.eventapp.sharedutils.security.ISecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.eventapp.bookingservice.enums.BookingStatus.CONFIRMED;
import static com.eventapp.bookingservice.enums.BookingStatus.EXPIRED;
import static com.eventapp.bookingservice.enums.BookingStatus.FAILED;
import static com.eventapp.bookingservice.enums.BookingStatus.PENDING;
import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.BOOKING_NOT_FOUND;
import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.RESERVATION_INVALID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements IBookingService {
    private final BookingRepository bookingRepository;
    private final OutboxEventRepository outboxRepository;
    private final BookingMapper bookingMapper;
    private final EventServiceGateway eventServiceGateway;
    private final ISecurityService securityService;
    private final IOutboxInventoryReleasesService outboxService;
    private final BookingOutboxFactory bookingOutboxFactory;
    private final ITicketCachePort ticketCachePort;
    private final TransactionTemplate transactionTemplate;
    private final IInventoryTtlProvider ttlProvider;

    private void executeSafeRelease(Long eventId,
                                    UUID bookingId,
                                    int quantity) {
        try {
            ticketCachePort.releaseReservedInventoryIdempotently(
                  eventId,
                  bookingId,
                  quantity
            );
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
        ticketCachePort.getGhostReservationMetadata(bookingId)
              .ifPresent(meta -> {
                  executeSafeRelease(
                        meta.eventId(),
                        bookingId,
                        meta.quantity()
                  );

                  ticketCachePort.cleanUpMetadataKey(bookingId);
              });
    }

    @Override
    public Booking getPendingBookingById(UUID id){
        return bookingRepository.findByIdAndStatus(id, PENDING)
              .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND, id));
    }

    @Override
    public BookingResponseDTO reserveTickets(BookingRequestDTO request) {
        String currentUserId = securityService.getAuthenticatedUserId();
        UUID bookingId = UUID.randomUUID();

        // Network call: Validate event and get price
        BigDecimal eventPrice = eventServiceGateway.getEventPrice(request.eventId());

        // Atomically decrement available and increment reserved
        ticketCachePort.reserveTicketWithLock(
              currentUserId,
              request.eventId(),
              bookingId,
              EventLifecycleStatus.PUBLISHED,
              request.quantity()
        );

        String userEmail = securityService.getUserEmail();

        Booking savedBooking =  transactionTemplate.execute(status -> {
            Booking booking = bookingMapper.toEntity(
                  request,
                  bookingId,
                  currentUserId,
                  userEmail,
                  eventPrice
            );

              return bookingRepository.save(booking);
        });

        // Ensure persistence succeeded before proceeding
        Objects.requireNonNull(savedBooking, "Booking failed to save within transaction");

        log.info("Booking {} reserved for user {}.", bookingId, currentUserId);

        int uiBufferSeconds = 30;

        return bookingMapper.toResponseDTO(
              savedBooking,
              ttlProvider.getLockTtlSeconds() - uiBufferSeconds
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
              PENDING,
              LocalDateTime.now()
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
    @Override
    public void confirmPurchase(String userId,
                                UUID bookingId,
                                Long amountCent) {
        // Fetch and Lock (Pessimistic lock to prevent race conditions)
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
              .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUserId().equals(userId))
            throw new ForbiddenException();

        if (booking.getStatus() == CONFIRMED) {
            log.info("Booking {} already confirmed. Ignoring.", bookingId);
            return;
        }

        booking.setStatus(CONFIRMED);
        bookingRepository.save(booking);

        OutboxEvent confirmedEvent = bookingOutboxFactory.buildConfirmedEvent(booking, amountCent);
        outboxRepository.save(confirmedEvent);

        // Update Redis
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    ticketCachePort.confirmPurchase(
                          userId,
                          booking.getEventId(),
                          bookingId,
                          EventLifecycleStatus.PUBLISHED,
                          booking.getQuantity()
                    );
                    log.info("Booking {} successfully synced to Redis", bookingId);
                } catch (Exception e) {
                    // Since Kafka won't retry, We log this as an ERROR for manual fix.
                    log.error("DB committed for booking {}, but REDIS UPDATE FAILED. " +
                          "System is now in inconsistent state! Error: {}", bookingId, e.getMessage(), e);
                }
            }
        });

        log.info("Booking {} marked as CONFIRMED in DB", bookingId);
    }

    @Transactional
    @Override
    public void processCompensation(String paymentId,
                                    String bookingId,
                                    String reason) {
        // Mark the booking as FAILED
        int updatedRows = bookingRepository.markAsFailed(
              UUID.fromString(bookingId),
              LocalDateTime.now()
        );

        // ONLY issue a refund if we successfully moved the record to FAILED
        if (updatedRows > 0) {
            // Save the Refund request to the Outbox table
            // Debezium will pick this up and send it to Kafka automatically
            OutboxEvent refundOutbox = bookingOutboxFactory.buildRefundEvent(
                  paymentId,
                  bookingId,
                  reason
            );

            outboxRepository.save(refundOutbox);
            log.info("Booking {} successfully marked as FAILED", bookingId);
        } else
            log.error("Refund suppressed for booking {}: Record already CONFIRMED or missing.", paymentId);
    }

    @Transactional
    public void handleAutomaticExpiration(UUID bookingId) {
        // Atomic update
        int rowsUpdated = bookingRepository.updateStatusToIfPending(
              bookingId,
              EXPIRED,
              LocalDateTime.now()
        );

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
            // Only proceed if the DB record is truly missing (Connection Loss scenario)
            handlePotentialGhostReservation(bookingId);
        }
    }

    @Override
    public void handleFailedPayment(UUID bookingId) {
        log.info("Cleaning up booking {} after payment failure", bookingId);

        int updated = bookingRepository.updateStatusToIfPending(
              bookingId,
              FAILED,
              LocalDateTime.now()
        );

        if (updated == 0) {
            log.info("Cleanup skipped: Booking {} already finalized.", bookingId);
            return;
        }

        Booking booking = bookingRepository.findById(bookingId)
              .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND, bookingId));

        // Release the held tickets
        ticketCachePort.releaseReservation(
              booking.getUserId(),
              booking.getEventId(),
              bookingId,
              booking.getQuantity()
        );

        log.info("Cleanup successful: Inventory released for failed booking {}.", bookingId);
    }
}