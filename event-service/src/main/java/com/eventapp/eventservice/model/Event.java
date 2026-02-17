package com.eventapp.eventservice.model;

import com.eventapp.eventservice.enums.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Title is required")
    @Column(nullable = false, unique = true)
    private String title;
    private String description;
    @Future(message = "Event date must be in the future")
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    @Column(nullable = false)
    private String location;
    @NotBlank
    @Column(name = "organizer_id", nullable = false)
    private String organizerId;

    @PositiveOrZero(message = "Price cannot be negative")
    @Column(nullable = false)
    private BigDecimal price;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private Integer capacity;

    private Integer soldCount = 0;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.PUBLISHED;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}