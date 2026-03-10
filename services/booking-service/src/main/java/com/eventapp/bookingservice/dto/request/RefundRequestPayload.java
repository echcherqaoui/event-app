package com.eventapp.bookingservice.dto.request;

import java.time.LocalDateTime;

public record RefundRequestPayload(String paymentId,
                                   String bookingId,
                                   String reason,
                                   LocalDateTime occurredAt){
}
