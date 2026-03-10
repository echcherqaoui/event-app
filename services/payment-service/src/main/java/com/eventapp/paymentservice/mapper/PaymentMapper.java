package com.eventapp.paymentservice.mapper;

import com.eventapp.contracts.payment.v1.PaymentEvent;
import com.eventapp.paymentservice.model.Payment;
import com.google.protobuf.util.Timestamps;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Instant;

@Component
public class PaymentMapper {

    public PaymentEvent toProtobuf(Payment payment) {
        long amountCents = payment.getAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        return PaymentEvent.newBuilder()
                .setPaymentId(payment.getId().toString())
                .setBookingId(payment.getBookingId().toString())
                .setCurrency("MAD")
                .setUserId(payment.getUserId())
                .setAmountCents(amountCents)
                .setOccurredAt(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                .build();
    }
}