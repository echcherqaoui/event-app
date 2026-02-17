package com.eventapp.bookingservice.model;

import com.eventapp.bookingservice.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter @Getter
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class Booking {
    @Id
    private UUID id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    @Min(value = 1, message = "Can't reserve more than 4 tickets")
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = BookingStatus.PENDING;
    }
}
