package com.eventapp.paymentservice.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRequestPayload(UUID paymentId,
                                    UUID bookingId,
                                    String userId,
                                    BigDecimal amount,
                                    LocalDateTime occurredAt) {
}
