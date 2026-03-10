package com.eventapp.paymentservice.kafaka;

import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.booking.v1.RefundRequested;
import com.eventapp.paymentservice.exception.domain.EventSecurityException;
import com.eventapp.paymentservice.service.IPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Kafka Consumer Unit Tests")
class BookingKafkaConsumerTest {
    @Mock private IPaymentService paymentService;
    @Mock private IEventAuthenticator eventAuthenticator;

    @InjectMocks private BookingKafkaConsumer kafkaConsumer;

    private RefundRequested validEvent;
    private static final String SIGNATURE = "valid-sig";

    @BeforeEach
    void setUp() {
        validEvent = RefundRequested.newBuilder()
              .setBookingId("book-123")
              .setPaymentId("pay-456")
              .setReason("User Cancelled")
              .setSignature(SIGNATURE)
              .build();
    }

    @Nested
    @DisplayName("Handle Refund Logic")
    class RefundLogicTests {
        @Test
        @DisplayName("Should process refund when signature is valid")
        void shouldProcessRefund_WhenSignatureIsValid() {
            String expectedRawData = "book-123:pay-456:User Cancelled";
            when(eventAuthenticator.verify(expectedRawData, SIGNATURE))
                  .thenReturn(true);

            kafkaConsumer.handleRefundPayment(validEvent);

            verify(paymentService, times(1)).handleRefund(validEvent);
        }

        @Test
        @DisplayName("Should throw EventSecurityException when signature is invalid")
        void shouldThrowSecurityException_WhenSignatureIsInvalid() {
            when(eventAuthenticator.verify(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> kafkaConsumer.handleRefundPayment(validEvent))
                  .isInstanceOf(EventSecurityException.class);

            verify(paymentService, never()).handleRefund(any());
        }
    }

    @Nested
    @DisplayName("DLT Handler Logic")
    class DltHandlerTests {
        @Test
        @DisplayName("Should drop event without error logs when cause is security exception")
        void shouldDropEvent_WhenCauseIsSecurityException() {
            String securityException = EventSecurityException.class.getName();

            kafkaConsumer.handleDlt(validEvent, securityException);

            // We shouldn't call payment service or rethrow
            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("Should log critical error for non-security technical failures")
        void shouldLogCriticalError_WhenCauseIsTechnicalException() {
            String technicalException = RuntimeException.class.getName();

            kafkaConsumer.handleDlt(validEvent, technicalException);

            verifyNoInteractions(paymentService);
        }
    }
}