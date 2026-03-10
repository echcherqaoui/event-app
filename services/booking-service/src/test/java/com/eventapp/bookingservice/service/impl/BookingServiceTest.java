package com.eventapp.bookingservice.service.impl;

import com.eventapp.bookingservice.client.EventServiceGateway;
import com.eventapp.bookingservice.dto.request.BookingRequestDTO;
import com.eventapp.bookingservice.dto.request.InventoryReleaseRequest;
import com.eventapp.bookingservice.mapper.BookingMapper;
import com.eventapp.bookingservice.outbox.BookingOutboxFactory;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.bookingservice.model.OutboxEvent;
import com.eventapp.bookingservice.repository.BookingRepository;
import com.eventapp.bookingservice.repository.OutboxEventRepository;
import com.eventapp.bookingservice.service.IOutboxInventoryReleasesService;
import com.eventapp.common.inventory.dto.GhostReservationMetadata;
import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.sharedutils.exceptions.domain.ForbiddenException;
import com.eventapp.sharedutils.security.ISecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.eventapp.bookingservice.enums.BookingStatus.CONFIRMED;
import static com.eventapp.bookingservice.enums.BookingStatus.EXPIRED;
import static com.eventapp.bookingservice.enums.BookingStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Service Resilience and Consistency Tests")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private EventServiceGateway eventServiceGateway;
    @Mock private ISecurityService securityService;
    @Mock private IOutboxInventoryReleasesService outboxService;
    @Mock private BookingOutboxFactory bookingOutboxFactory;
    @Mock private ITicketCachePort ticketCachePort;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private IInventoryTtlProvider ttlProvider;

    @InjectMocks private BookingServiceImpl bookingService;

    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final String USER_ID = "user-777";
    private static final Long EVENT_ID = 1L;
    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = new Booking()
              .setId(BOOKING_ID)
              .setUserId(USER_ID)
              .setEventId(EVENT_ID)
              .setQuantity(2)
              .setStatus(PENDING);

        TransactionStatus status = mock(TransactionStatus.class);

        lenient().when(transactionTemplate.execute(any()))
              .thenAnswer(invocation -> {
                  TransactionCallback<?> callback = invocation.getArgument(0);
                  return callback.doInTransaction(status);
              });
    }

    @AfterEach
    void tearDown() {
        // Cleanup synchronization context if it was initialized
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clear();
    }

    @Nested
    @DisplayName("Ticket Reservation Flow")
    class ReservationTests {

        @Test
        @DisplayName("Should reserve in Redis first then persist to DB")
        void shouldReserveInRedis_BeforePersistingToDatabase() {
            BookingRequestDTO request = new BookingRequestDTO(EVENT_ID, 2);

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(USER_ID);

            when(eventServiceGateway.getEventPrice(EVENT_ID))
                  .thenReturn(BigDecimal.TEN);

            when(bookingMapper.toEntity(any(), any(), any(), any(), any()))
                  .thenReturn(booking);

            when(bookingRepository.save(any()))
                  .thenReturn(booking);

            when(ttlProvider.getLockTtlSeconds())
                  .thenReturn(600L);


            bookingService.reserveTickets(request);


            verify(ticketCachePort).reserveTicketWithLock(eq(USER_ID), eq(EVENT_ID), any(UUID.class), any(), eq(2));

            verify(bookingRepository).save(any(Booking.class));
        }
    }

    @Nested
    @DisplayName("Purchase Confirmation and Consistency")
    class ConfirmationTests {

        @BeforeEach
        void initTransaction() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @Test
        @DisplayName("Should transition status to CONFIRMED and write to Outbox")
        void shouldUpdateStatusAndWriteToOutbox_OnPurchase() {
            when(bookingRepository.findByIdWithLock(BOOKING_ID))
                  .thenReturn(Optional.of(booking));

            when(bookingOutboxFactory.buildConfirmedEvent(any(), anyLong()))
                  .thenReturn(new OutboxEvent());

            bookingService.confirmPurchase(USER_ID, BOOKING_ID, 1000L);

            assertThat(booking.getStatus()).isEqualTo(CONFIRMED);
            verify(outboxRepository).save(any(OutboxEvent.class));

            // Verify sync was registered
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail when a different user tries to confirm a booking")
        void should_ThrowForbidden_When_UnauthorizedUserConfirms() {
            when(bookingRepository.findByIdWithLock(BOOKING_ID))
                  .thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.confirmPurchase("wrong-user", BOOKING_ID, 1000L))
                  .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Expiration and Ghost Reservation Handling")
    class ExpirationTests {

        @Test
        @DisplayName("Should release inventory via Outbox if Redis is down during expiration")
        void shouldUseOutboxFallback_WhenRedisFailsDuringExpiration() {
            when(bookingRepository.updateStatusToIfPending(eq(BOOKING_ID), eq(EXPIRED), any()))
                  .thenReturn(1);

            when(bookingRepository.findById(BOOKING_ID))
                  .thenReturn(Optional.of(booking));

            doThrow(new RuntimeException("Redis connection lost"))
                  .when(ticketCachePort).releaseReservedInventoryIdempotently(anyLong(), any(UUID.class), anyInt());

            bookingService.handleAutomaticExpiration(BOOKING_ID);

            verify(outboxService).saveToOutbox(any(InventoryReleaseRequest.class));
        }

        @Test
        @DisplayName("Should handle ghost reservations if DB record is missing during expiration")
        void shouldCleanupGhostReservation_WhenDatabaseRecordIsMissing() {
            when(bookingRepository.updateStatusToIfPending(eq(BOOKING_ID), eq(EXPIRED), any()))
                  .thenReturn(0);

            GhostReservationMetadata meta = new GhostReservationMetadata(EVENT_ID, 2);
            when(ticketCachePort.getGhostReservationMetadata(BOOKING_ID))
                  .thenReturn(Optional.of(meta));

            bookingService.handleAutomaticExpiration(BOOKING_ID);

            verify(ticketCachePort).releaseReservedInventoryIdempotently(EVENT_ID, BOOKING_ID, 2);
            verify(ticketCachePort).cleanUpMetadataKey(BOOKING_ID);
        }
    }

    @Nested
    @DisplayName("Compensation and Refunds")
    class CompensationTests {

        @Test
        @DisplayName("Should save refund to Outbox only if booking was successfully marked as FAILED")
        void shouldWriteRefundToOutbox_OnlyOnStatusTransitionSuccess() {
            when(bookingRepository.markAsFailed(eq(BOOKING_ID), any()))
                  .thenReturn(1);

            when(bookingOutboxFactory.buildRefundEvent(any(), any(), any()))
                  .thenReturn(new OutboxEvent());

            bookingService.processCompensation("pay-1", BOOKING_ID.toString(), "timeout");

            verify(outboxRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should skip refund if booking is already final")
        void shouldSkipRefund_WhenStatusUpdateReturnsZeroRows() {
            when(bookingRepository.markAsFailed(eq(BOOKING_ID), any()))
                  .thenReturn(0);

            bookingService.processCompensation("pay-1", BOOKING_ID.toString(), "timeout");

            verify(outboxRepository, never()).save(any());
        }
    }
}