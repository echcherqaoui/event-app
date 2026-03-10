package com.eventapp.paymentservice.outbox;

import com.eventapp.contracts.payment.v1.PaymentEvent;
import com.eventapp.paymentservice.kafaka.serializer.ProtobufOutboxService;
import com.eventapp.paymentservice.mapper.PaymentMapper;
import com.eventapp.paymentservice.model.OutboxEvent;
import com.eventapp.paymentservice.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventFactory {
    private final ProtobufOutboxService protobufService;
    private final PaymentMapper paymentMapper;

    private OutboxEvent createOutbox(String aggregateId,
                                     String eventType,
                                     byte[] serializedPayload) {

        return new OutboxEvent()
              .setAggregateId(aggregateId)
              .setAggregateType("Payment")
              .setEventType(eventType)
              .setPayload(serializedPayload);
    }

    public OutboxEvent buildEvent(Payment payment,
                                  String eventType) {
        PaymentEvent paymentEvent = paymentMapper.toProtobuf(payment);

        byte[] serializedPayload = protobufService.serialize(eventType, paymentEvent);

        return createOutbox(
              payment.getId().toString(),
              eventType,
              serializedPayload
        );
    }
}