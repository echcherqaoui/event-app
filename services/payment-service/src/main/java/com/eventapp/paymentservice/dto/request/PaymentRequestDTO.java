package com.eventapp.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentRequestDTO(@NotNull UUID bookingId,
                                @NotBlank String paymentMethodId) {
}
