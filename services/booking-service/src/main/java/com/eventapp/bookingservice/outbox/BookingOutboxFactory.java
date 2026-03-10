package com.eventapp.bookingservice.outbox;

import com.eventapp.bookingservice.kafka.serializer.ProtobufOutboxService;
import com.eventapp.bookingservice.mapper.BookingOutboxMapper;
import com.eventapp.bookingservice.model.Booking;
import com.eventapp.bookingservice.model.OutboxEvent;
import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.eventapp.contracts.booking.v1.RefundRequested;
import com.google.protobuf.util.Timestamps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class BookingOutboxFactory {
    private final ProtobufOutboxService protobufOutboxService;
    private static final String AGGREGATE_BOOKING = "Booking";
    private final BookingOutboxMapper mapper;

    private OutboxEvent createOutbox(String aggregateId,
                                     String eventType,
                                     Supplier<byte[]> serializationTask) {
        return new OutboxEvent()
              .setAggregateId(aggregateId)
              .setAggregateType(AGGREGATE_BOOKING)
              .setEventType(eventType)
              .setPayload(serializationTask.get()); // Execute the specific serialization logic
    }

    public OutboxEvent buildRefundEvent(String paymentId,
                                        String bookingId,
                                        String reason) {
        RefundRequested payload = RefundRequested.newBuilder()
              .setBookingId(bookingId)
              .setPaymentId(paymentId)
              .setReason(reason)
              .setOccurredAt(Timestamps.fromMillis(Instant.now().toEpochMilli()))
              .build();

        String eventType = "PAYMENT_REFUND_REQUESTED";

        String topic =  "booking.events.%s".formatted(eventType);

        return createOutbox(
              bookingId,
              eventType,
              () -> protobufOutboxService.serializeRefund(topic, payload) // Pass the explicit serialization call as a lambda
        );
    }

    public OutboxEvent buildConfirmedEvent(Booking booking, long amount) {
        BookingConfirmed payload = mapper.toConfirmedProto(
              booking,
              amount,
              Instant.now()
        );

        String eventType = "CONFIRMED";
        String topic =  "booking.events.%s".formatted(eventType);

        return createOutbox(
              booking.getId().toString(),
              eventType,
              () -> protobufOutboxService.serializeConfirmation(topic, payload)
        );
    }
}