package com.eventapp.bookingservice.kafka;

import com.eventapp.bookingservice.exceptions.domain.EventSecurityException;
import com.eventapp.bookingservice.service.IBookingService;
import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.payment.v1.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaConsumer {
    private final IBookingService bookingService;
    private final IEventAuthenticator eventAuthenticator;

    private void validateEvent(PaymentEvent event) {
        String rawData = String.format("%s:%s:%s:%d",
              event.getBookingId(),
              event.getPaymentId(),
              event.getUserId(),
              event.getAmountCents()
        );

        if (!eventAuthenticator.verify(rawData, event.getSignature())) {
            log.error("SECURITY ALERT: Invalid signature for Booking: {}", event.getBookingId());
            throw new EventSecurityException(event.getBookingId());
        }
    }

    @KafkaListener(topics = "payment.events.PAYMENT_COMPLETED", groupId = "booking-service-group")
    public void handlePaymentCompleted(PaymentEvent event) {
        validateEvent(event);
        bookingService.confirmPurchase(
              event.getUserId(),
              UUID.fromString(event.getBookingId()),
              event.getAmountCents()
        );
    }

    @KafkaListener(topics = "payment.events.PAYMENT_FAILED", groupId = "booking-service-group")
    public void handlePaymentFailed(PaymentEvent event) {
        validateEvent(event);
        bookingService.handleFailedPayment(UUID.fromString(event.getBookingId()));
    }

    @DltHandler
    public void handleDlt(PaymentEvent payment, @Header(KafkaHeaders.EXCEPTION_CAUSE_FQCN) String causeExceptionType) {
        if (EventSecurityException.class.getName().equals(causeExceptionType)) {
            log.error("SECURITY: Dropped malicious event: {}", payment.getPaymentId());
            return; // ignore
        }

        log.error("CRITICAL: Booking confirmation failed. Emitting refund event.");

        bookingService.processCompensation(
              payment.getPaymentId(),
              payment.getBookingId(),
              "Payment received for non-confirmable booking"
        );
    }
}