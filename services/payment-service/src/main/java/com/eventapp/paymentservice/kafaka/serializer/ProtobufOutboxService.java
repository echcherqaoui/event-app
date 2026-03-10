package com.eventapp.paymentservice.kafaka.serializer;

import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.payment.v1.PaymentEvent;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProtobufOutboxService {
    private final KafkaProtobufSerializer<PaymentEvent> serializer;
    private final IEventAuthenticator eventAuthenticator;

    public byte[] serialize(String eventType, PaymentEvent event) {
        String topic =  "payment.events.%s".formatted(eventType);

        String dataToSign = String.format("%s:%s:%s:%d",
              event.getBookingId(),
              event.getPaymentId(),
              event.getUserId(),
              event.getAmountCents()
        );

        String signature = eventAuthenticator.sign(dataToSign);

        PaymentEvent securedEvent = event.toBuilder()
              .setSignature(signature)
              .build();

        return serializer.serialize(topic, securedEvent);
    }
}