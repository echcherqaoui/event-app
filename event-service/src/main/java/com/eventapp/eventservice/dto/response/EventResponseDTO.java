package com.eventapp.eventservice.dto.response;

import com.eventapp.eventservice.enums.EventStatus;
import com.eventapp.eventservice.model.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponseDTO(Long id,
                               String title,
                               String description,
                               LocalDateTime eventDate,
                               String location,
                               String organizerId,
                               BigDecimal price,
                               Integer capacity,
                               EventStatus status) {

    public static EventResponseDTO from(Event event) {
        return new EventResponseDTO(
              event.getId(),
              event.getTitle(),
              event.getDescription(),
              event.getEventDate(),
              event.getLocation(),
              event.getOrganizerId(),
              event.getPrice(),
              event.getCapacity(),
              event.getStatus()
        );
    }
}