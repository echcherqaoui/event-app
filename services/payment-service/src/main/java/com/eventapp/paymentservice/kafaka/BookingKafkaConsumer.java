package com.eventapp.paymentservice.kafaka;

import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.booking.v1.RefundRequested;
import com.eventapp.paymentservice.exception.domain.EventSecurityException;
import com.eventapp.paymentservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingKafkaConsumer {
    private final IPaymentService paymentService;
    private final IEventAuthenticator eventAuthenticator;

    @KafkaListener(topics = "booking.events.PAYMENT_REFUND_REQUESTED", groupId = "payment-service-group")
    public void handleRefundPayment(RefundRequested event) {
        String rawData = String.format("%s:%s:%s",
              event.getBookingId(),
              event.getPaymentId(),
              event.getReason());

        if (!eventAuthenticator.verify(rawData, event.getSignature())) {
            log.error("SECURITY ALERT: Invalid signature for Payment: {}", event.getBookingId());
            throw new EventSecurityException(event.getBookingId());
        }

        paymentService.handleRefund(event);
    }

    @DltHandler
    public void handleDlt(RefundRequested event, @Header(KafkaHeaders.EXCEPTION_CAUSE_FQCN) String causeExceptionType) {
        if (EventSecurityException.class.getName().equals(causeExceptionType)) {
            log.error("SECURITY: Dropped malicious event: {}", event.getPaymentId());
            return; // ignore
        }

        log.error("CRITICAL: Refund processing failed for Payment ID: {}. Manual intervention required.",
              event.getPaymentId());
    }
}