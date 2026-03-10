package com.eventapp.paymentservice.service;

import com.eventapp.paymentservice.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface IGatewayService {

    void charge(String paymentMethodId,
                BigDecimal amount);

    PaymentStatus getRemoteStatus(UUID bookingId);
}