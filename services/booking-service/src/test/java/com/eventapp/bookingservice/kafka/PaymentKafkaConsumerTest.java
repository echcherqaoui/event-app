package com.eventapp.bookingservice.kafka;

import com.eventapp.bookingservice.exceptions.domain.EventSecurityException;
import com.eventapp.bookingservice.service.IBookingService;
import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.payment.v1.PaymentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Kafka Consumer Unit Tests")
class PaymentKafkaConsumerTest {

    @Mock private IBookingService bookingService;
    @Mock private IEventAuthenticator eventAuthenticator;
    @InjectMocks private PaymentKafkaConsumer paymentKafkaConsumer;

    private PaymentEvent validEvent;
    private static final String BOOKING_ID = UUID.randomUUID().toString();
    private static final String USER_ID = "user-123";
    private static final String SIGNATURE = "valid-sig-789";

    @BeforeEach
    void setUp() {
        validEvent = PaymentEvent.newBuilder()
              .setBookingId(BOOKING_ID)
              .setPaymentId("pay-999")
              .setUserId(USER_ID)
              .setAmountCents(5000L)
              .setSignature(SIGNATURE)
              .build();
    }

    @Nested
    @DisplayName("Payment Event Processing")
    class PaymentProcessingTests {
        @Test
        @DisplayName("Should confirm purchase when signature and data are valid")
        void handlePaymentCompleted_Success() {
            when(eventAuthenticator.verify(anyString(), eq(SIGNATURE)))
                  .thenReturn(true);

            paymentKafkaConsumer.handlePaymentCompleted(validEvent);

            verify(bookingService).confirmPurchase(USER_ID, UUID.fromString(BOOKING_ID), 5000L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when BookingID is not a UUID")
        void handlePaymentCompleted_InvalidUuid() {
            PaymentEvent malformedEvent = PaymentEvent.newBuilder(validEvent)
                  .setBookingId("not-a-uuid")
                  .build();

            when(eventAuthenticator.verify(anyString(), anyString()))
                  .thenReturn(true);

            assertThatThrownBy(() -> paymentKafkaConsumer.handlePaymentCompleted(malformedEvent))
                  .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Security & Signature Validation")
    class SecurityTests {

        @Test
        @DisplayName("Should throw EventSecurityException and block execution when signature is invalid")
        void handlePaymentCompleted_SecurityAlert() {
            when(eventAuthenticator.verify(anyString(), eq(SIGNATURE)))
                  .thenReturn(false);

            assertThatThrownBy(() -> paymentKafkaConsumer.handlePaymentCompleted(validEvent))
                  .isInstanceOf(EventSecurityException.class);

            verify(bookingService, never()).confirmPurchase(any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("Dead Letter Topic (DLT) Recovery Logic")
    class DltRecoveryTests {

        @Test
        @DisplayName("Should drop message without refunding when cause is a security violation")
        void shouldIgnore_WhenCauseIsSecurityException() {
            String cause = EventSecurityException.class.getName();

            paymentKafkaConsumer.handleDlt(validEvent, cause);

            verify(bookingService, never()).processCompensation(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should trigger compensation/refund for non-security processing errors")
        void shouldRefund_WhenCauseIsProcessingError() {
            String cause = RuntimeException.class.getName();

            paymentKafkaConsumer.handleDlt(validEvent, cause);

            verify(bookingService).processCompensation(
                  eq(validEvent.getPaymentId()),
                  eq(validEvent.getBookingId()),
                  anyString()
            );
        }
    }
}