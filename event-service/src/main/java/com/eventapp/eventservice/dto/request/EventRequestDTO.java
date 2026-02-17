package com.eventapp.eventservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequestDTO(@NotBlank(message = "Title is required") String title,
                              @Size(max = 2000) String description,
                              @NotNull(message = "Date is required")
                              @Future(message = "Date must be in the future") LocalDateTime eventDate,
                              @NotBlank(message = "Location is required") String location,
                              @Positive(message = "Price must be positive") BigDecimal price,
                              @NotNull(message = "capacity is required")
                              @Min(value = 1, message = "Capacity must be at least 1") Integer capacity) {}