package com.eventapp.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(UUID bookingId,
                                    Long eventId,
                                    String userId,
                                    int quantity,
                                    BigDecimal amount,
                                    LocalDateTime timestamp) {
}