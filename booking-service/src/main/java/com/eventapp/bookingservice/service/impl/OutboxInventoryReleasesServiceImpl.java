package com.eventapp.bookingservice.service.impl;

import com.eventapp.bookingservice.dto.request.InventoryReleaseRequest;
import com.eventapp.bookingservice.model.OutboxInventoryRelease;
import com.eventapp.bookingservice.repository.OutboxInventoryReleaseRepository;
import com.eventapp.bookingservice.service.IOutboxInventoryReleasesService;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxInventoryReleasesServiceImpl implements IOutboxInventoryReleasesService {
    private final OutboxInventoryReleaseRepository outboxRepository;
    private final ITicketCachePort inventoryService;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 10;

    private void processPendingJob(OutboxInventoryRelease job) {
        try {
            log.debug("Retrying release for Booking ID: {}", job.getBookingId());

            //  Attempt the Idempotent Release
            inventoryService.releaseReservedInventoryIdempotently(
                  job.getEventId(),
                  job.getBookingId(),
                  job.getQuantity()
            );

            job.setProcessed(true);
            log.info("Outbox Success: Released inventory for Booking {}", job.getBookingId());
        } catch (Exception e) {
            // Failure: Increment retry count
            job.setRetryCount(job.getRetryCount() + 1);
            log.warn("Outbox Failed: Booking {}. Retry count now {}. Error: {}", job.getBookingId(), job.getRetryCount(), e.getMessage());

            // 5. Critical Alerting (This effectively acts as a Dead Letter Queue)
            if (job.getRetryCount() >= MAX_RETRIES)
                log.error("CRITICAL ALERT: Booking {} has failed {} retries. Inventory is stuck. Manual Intervention Required.", job.getBookingId(), MAX_RETRIES);
        }

        // Save the updated state (processed=true OR incremented retryCount)
        outboxRepository.save(job);
    }

     // Called by BookingService when immediate Redis release fails.
     @Transactional(propagation = Propagation.REQUIRES_NEW)
     @Override
     public void saveToOutbox(InventoryReleaseRequest releaseRequest) {
        OutboxInventoryRelease outBoxRecord = new OutboxInventoryRelease()
              .setBookingId(releaseRequest.bookingId())
              .setEventId(releaseRequest.eventId())
              .setQuantity(releaseRequest.quantity());

        outboxRepository.save(outBoxRecord);
    }

    @Scheduled(fixedDelay = 180_000) //Every 3min
    @Transactional
    @Override
    public void processOutboxFailures() {
        // Find oldest 50 records that aren't processed and haven't hit 10 retries
        List<OutboxInventoryRelease> pendingJobs = outboxRepository.findPendingRetries(
              MAX_RETRIES,
              PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingJobs.isEmpty()) return;

        log.info("Outbox Reaper: Found {} pending inventory releases.", pendingJobs.size());

        pendingJobs.forEach(this::processPendingJob);
    }
}
