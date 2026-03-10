package com.eventapp.notificationservice.kafka;

import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.eventapp.notificationservice.exception.domain.EventSecurityException;
import com.eventapp.notificationservice.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final IEventAuthenticator eventAuthenticator;
    private final IEmailService emailService;

    @KafkaListener(
          topics = "booking.events.CONFIRMED",
          groupId = "notification-service-group"
    )
    public void handleRefundRequested(BookingConfirmed event,
                                      @Header(KafkaHeaders.RECEIVED_KEY) String messageId) {
        log.info("Processing Booking Confirmation: {} (Message: {})", event.getBookingId(), messageId);

        String rawData = String.format("%s:%s:%s:%s",
              event.getBookingId(),
              event.getEventId(),
              event.getUserId(),
              event.getUserEmail()
        );

        if (!eventAuthenticator.verify(rawData, event.getSignature())) {
            log.error("SECURITY ALERT: Tampered signature for Booking: {}", event.getBookingId());

            throw new EventSecurityException(event.getBookingId()); //Excluded, No retry at all
        }

        emailService.sendBookingEmail(event, messageId);

        log.info("Successfully handled confirmation for Booking: {}", event.getBookingId());
    }

    @DltHandler
    public void handleDlt(BookingConfirmed event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("CRITICAL: Booking Confirmation {} failed all retries. Moved to DLT: {}",
              event.getBookingId(), topic);
    }
}