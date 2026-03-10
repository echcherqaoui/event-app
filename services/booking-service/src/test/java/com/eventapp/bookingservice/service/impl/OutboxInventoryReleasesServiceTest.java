package com.eventapp.bookingservice.service.impl;

import com.eventapp.bookingservice.dto.request.InventoryReleaseRequest;
import com.eventapp.bookingservice.model.OutboxInventoryRelease;
import com.eventapp.bookingservice.repository.OutboxInventoryReleaseRepository;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox Inventory Release Service Tests")
class OutboxInventoryReleasesServiceTest {

    @Mock private OutboxInventoryReleaseRepository outboxRepository;
    @Mock private ITicketCachePort inventoryService;

    @InjectMocks private OutboxInventoryReleasesServiceImpl outboxService;

    private OutboxInventoryRelease pendingJob;
    private static final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pendingJob = new OutboxInventoryRelease()
                .setBookingId(bookingId)
                .setEventId(202L)
                .setQuantity(2)
                .setRetryCount(0)
                .setProcessed(false);
    }

    @Nested
    @DisplayName("Outbox Ingestion")
    class IngestionTests {
        @Test
        @DisplayName("Should save a new outbox record with provided request details")
        void shouldSaveNewRecordToOutbox() {
            InventoryReleaseRequest request = new InventoryReleaseRequest(bookingId, 202L, 2);

            outboxService.saveToOutbox(request);

            ArgumentCaptor<OutboxInventoryRelease> captor = ArgumentCaptor.forClass(OutboxInventoryRelease.class);
            verify(outboxRepository).save(captor.capture());
            
            OutboxInventoryRelease saved = captor.getValue();
            assertThat(saved.getBookingId()).isEqualTo(bookingId);
            assertThat(saved.getProcessed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Reaper Processing Logic")
    class ReaperTests {

        @Test
        @DisplayName("Should mark job as processed when Redis release succeeds")
        void shouldMarkAsProcessed_WhenReleaseSucceeds() {
            when(outboxRepository.findPendingRetries(anyInt(), any(Pageable.class)))
                    .thenReturn(List.of(pendingJob));

            outboxService.processOutboxFailures();

            verify(inventoryService).releaseReservedInventoryIdempotently(202L, bookingId, 2);
            assertThat(pendingJob.getProcessed()).isTrue();
            verify(outboxRepository).save(pendingJob);
        }

        @Test
        @DisplayName("Should increment retry count when Redis release throws an exception")
        void shouldIncrementRetry_WhenReleaseFails() {
            when(outboxRepository.findPendingRetries(anyInt(), any(Pageable.class)))
                  .thenReturn(List.of(pendingJob));

            doThrow(new RuntimeException("Redis Down"))
                  .when(inventoryService)
                  .releaseReservedInventoryIdempotently(anyLong(), any(UUID.class), anyInt());

            outboxService.processOutboxFailures();

            assertThat(pendingJob.getRetryCount()).isEqualTo(1);
            assertThat(pendingJob.getProcessed()).isFalse();
            verify(outboxRepository).save(pendingJob);
        }

        @Test
        @DisplayName("Should respect max retries and skip processing when no jobs are found")
        void shouldDoNothing_WhenNoPendingJobs() {
            when(outboxRepository.findPendingRetries(anyInt(), any(Pageable.class)))
                    .thenReturn(List.of());

            outboxService.processOutboxFailures();

            verify(inventoryService, never()).releaseReservedInventoryIdempotently(anyLong(), any(UUID.class), anyInt());
            verify(outboxRepository, never()).save(any());
        }
    }
}