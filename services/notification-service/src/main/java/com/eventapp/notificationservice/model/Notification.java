package com.eventapp.notificationservice.model;

import com.eventapp.notificationservice.enums.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

import static com.eventapp.notificationservice.enums.NotificationStatus.PENDING;

@Entity
@Getter @Setter
@NoArgsConstructor
@Accessors(chain = true)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String messageId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String bookingId;

    private String recipient;

    private String subject;
    
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(nullable = false)
    private LocalDateTime sentAt;


    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
        this.status = PENDING;
    }
}

