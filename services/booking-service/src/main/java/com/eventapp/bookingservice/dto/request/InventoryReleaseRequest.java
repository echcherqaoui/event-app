package com.eventapp.bookingservice.dto.request;

import java.util.UUID;

public record InventoryReleaseRequest(UUID bookingId,
                                      Long eventId,
                                      Integer quantity) {
}