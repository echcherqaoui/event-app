package com.eventapp.paymentservice.service;

import com.eventapp.contracts.booking.v1.RefundRequested;
import com.eventapp.paymentservice.dto.request.PaymentRequestDTO;
import com.eventapp.paymentservice.enums.PaymentStatus;
import com.eventapp.paymentservice.model.Payment;

public interface IPaymentService {
    void finalizePayment(Payment payment,
                         PaymentStatus newStatus,
                         String eventType);

    void processPayment(PaymentRequestDTO request);

    void handleRefund(RefundRequested event);
}
