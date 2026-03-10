package com.eventapp.bookingservice.kafka.serializer;

import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.eventapp.contracts.booking.v1.RefundRequested;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProtobufOutboxService {
    private final KafkaProtobufSerializer<RefundRequested> refundSerializer;
    private final KafkaProtobufSerializer<BookingConfirmed> confirmedSerializer;
    private final IEventAuthenticator eventAuthenticator;

    public byte[] serializeRefund(String topic, RefundRequested event) {
        String dataToSign = String.format("%s:%s:%s",
              event.getBookingId(),
              event.getPaymentId(),
              event.getReason());

        String signature = eventAuthenticator.sign(dataToSign);

        RefundRequested securedEvent = event.toBuilder()
              .setSignature(signature)
              .build();

        return refundSerializer.serialize(topic, securedEvent);
    }

    public byte[] serializeConfirmation(String topic, BookingConfirmed event) {

        String dataToSign = String.format("%s:%s:%s:%s",
              event.getBookingId(),
              event.getEventId(),
              event.getUserId(),
              event.getUserEmail()
        );

        BookingConfirmed secured = event.toBuilder()
              .setSignature(eventAuthenticator.sign(dataToSign))
              .build();

        return confirmedSerializer.serialize(topic, secured);
    }
}