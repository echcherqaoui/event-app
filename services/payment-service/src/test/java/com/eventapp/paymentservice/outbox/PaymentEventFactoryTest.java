package com.eventapp.paymentservice.outbox;

import com.eventapp.contracts.payment.v1.PaymentEvent;
import com.eventapp.paymentservice.kafaka.serializer.ProtobufOutboxService;
import com.eventapp.paymentservice.mapper.PaymentMapper;
import com.eventapp.paymentservice.model.OutboxEvent;
import com.eventapp.paymentservice.model.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Event Factory Unit Tests")
class PaymentEventFactoryTest {

    @Mock private ProtobufOutboxService protobufService;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks private PaymentEventFactory eventFactory;

    @Test
    @DisplayName("Should build a valid OutboxEvent from Payment and EventType")
    void shouldReturnPopulatedOutboxEvent() {
        UUID paymentId = UUID.randomUUID();
        String eventType = "PAYMENT_COMPLETED";
        byte[] mockPayload = "serialized-data".getBytes();

        Payment payment = new Payment()
              .setId(paymentId)
              .setAmount(BigDecimal.TEN)
              .setBookingId(UUID.randomUUID());

        PaymentEvent protoEvent = PaymentEvent.newBuilder()
              .setPaymentId(paymentId.toString())
              .build();

        when(paymentMapper.toProtobuf(payment))
              .thenReturn(protoEvent);

        when(protobufService.serialize(eq(eventType), any(PaymentEvent.class)))
              .thenReturn(mockPayload);

        OutboxEvent result = eventFactory.buildEvent(payment, eventType);

        assertThat(result).isNotNull();
        assertThat(result.getAggregateId()).isEqualTo(paymentId.toString());
        assertThat(result.getAggregateType()).isEqualTo("Payment");
        assertThat(result.getEventType()).isEqualTo(eventType);
        assertThat(result.getPayload()).isEqualTo(mockPayload);

        // Verify interactions
        verify(paymentMapper).toProtobuf(payment);
        verify(protobufService).serialize(eventType, protoEvent);
    }
}