package com.eventapp.bookingservice.service;

import com.eventapp.bookingservice.dto.request.InventoryReleaseRequest;

public interface IOutboxInventoryReleasesService {
   void saveToOutbox(InventoryReleaseRequest releaseRequest);

    void processOutboxFailures();
}
