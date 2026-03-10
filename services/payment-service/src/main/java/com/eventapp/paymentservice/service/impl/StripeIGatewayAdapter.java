package com.eventapp.paymentservice.service.impl;

import com.eventapp.paymentservice.enums.PaymentStatus;
import com.eventapp.paymentservice.exception.domain.GatewayTimeoutException;
import com.eventapp.paymentservice.exception.domain.PaymentDeclinedException;
import com.eventapp.paymentservice.service.IGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.eventapp.paymentservice.enums.PaymentStatus.COMPLETED;
import static com.eventapp.paymentservice.enums.PaymentStatus.FAILED;

@Service
@Slf4j
public class StripeIGatewayAdapter implements IGatewayService {

    private final double failureRate;

    public StripeIGatewayAdapter(@Value("${payment.simulation.failure-rate:0.01}") double failureRate) {
        this.failureRate = failureRate;
    }

    @Override
    public void charge(String paymentMethodId, BigDecimal amount) {
        log.info("Simulating Stripe charge: method={}, amount={}", paymentMethodId, amount);

        if ("CARD_DECLINED".equals(paymentMethodId)) throw new PaymentDeclinedException();
        if ("GATEWAY_TIMEOUT".equals(paymentMethodId)) throw new GatewayTimeoutException();

        double r = ThreadLocalRandom.current().nextDouble();

        if (r < failureRate) {
            if (r < failureRate / 2)
                throw new GatewayTimeoutException();

            throw new PaymentDeclinedException();
        }

        log.info("Stripe successful for amount {}", amount);
    }

    @Override
    public PaymentStatus getRemoteStatus(UUID bookingId) {
        log.info("Querying external gateway for status of Booking: {}", bookingId);
        double chance = Math.random();

        return chance < 0.8? COMPLETED: FAILED;
    }
}