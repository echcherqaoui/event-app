package com.eventapp.bookingservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponseDTO(
    UUID bookingId,
    String status,
    LocalDateTime expiresAt
) {}