package com.eventapp.paymentservice.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundInitiatedEvent(UUID bookingId,
                                   String reason,
                                   LocalDateTime timestamp) {}