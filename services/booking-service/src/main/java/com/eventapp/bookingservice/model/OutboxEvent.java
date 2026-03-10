package com.eventapp.bookingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event", indexes = {
      @Index(name = "idx_outbox_aggregate_id", columnList = "aggregateId"),
      @Index(name = "idx_outbox_created_at", columnList = "createdAt")
})
@Getter
@Setter
@Accessors(chain = true)
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String aggregateId;

    @Column(nullable = false, updatable = false)
    private String aggregateType;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false)
    private byte[] payload;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}