package com.eventapp.bookingservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequestDTO(
    @NotNull Long eventId,
    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 4, message = "Can't reserve more than 4 tickets") int quantity
) {}