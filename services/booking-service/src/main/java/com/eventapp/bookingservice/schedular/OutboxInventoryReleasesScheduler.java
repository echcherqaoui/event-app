package com.eventapp.bookingservice.schedular;

import com.eventapp.bookingservice.service.IOutboxInventoryReleasesService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxInventoryReleasesScheduler {

    private final IOutboxInventoryReleasesService service;

    @Scheduled(fixedDelayString = "PT3M")
    public void scheduleRetryProcessing() {
        service.processOutboxFailures();
    }
}