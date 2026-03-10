package com.eventapp.paymentservice.service.impl;

import com.eventapp.contracts.booking.v1.RefundRequested;
import com.eventapp.lib.booking.v1.BookingResponse;
import com.eventapp.paymentservice.client.BookingServiceGateway;
import com.eventapp.paymentservice.dto.request.PaymentRequestDTO;
import com.eventapp.paymentservice.enums.PaymentStatus;
import com.eventapp.paymentservice.exception.domain.PaymentAlreadyProcessedException;
import com.eventapp.paymentservice.exception.domain.PaymentDeclinedException;
import com.eventapp.paymentservice.outbox.PaymentEventFactory;
import com.eventapp.paymentservice.model.Payment;
import com.eventapp.paymentservice.repository.OutboxEventRepository;
import com.eventapp.paymentservice.repository.PaymentRepository;
import com.eventapp.paymentservice.service.IGatewayService;
import com.eventapp.paymentservice.service.IPaymentService;
import com.eventapp.sharedutils.exceptions.domain.BusinessException;
import com.eventapp.sharedutils.exceptions.domain.ForbiddenException;
import com.eventapp.sharedutils.security.ISecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.eventapp.paymentservice.enums.PaymentStatus.COMPLETED;
import static com.eventapp.paymentservice.enums.PaymentStatus.FAILED;
import static com.eventapp.paymentservice.enums.PaymentStatus.PENDING;
import static com.eventapp.paymentservice.enums.PaymentStatus.REFUNDED;
import static com.eventapp.paymentservice.exception.enums.ErrorCode.GATEWAY_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockPaymentServiceImpl implements IPaymentService {
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final IGatewayService gatewayService;
    private final ISecurityService securityService;
    private final BookingServiceGateway bookingServiceGateway;
    private final PaymentEventFactory paymentEventFactory;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void finalizePayment(Payment payment,
                                PaymentStatus newStatus,
                                String eventType) {
        transactionTemplate.executeWithoutResult(s -> {
            int updated = paymentRepository.updateStatusAtomic(
                  payment.getId(),
                  LocalDateTime.now(),
                  PENDING,
                  newStatus
            );

            if (updated == 0) {
                log.warn("Payment {} already finalized or state changed. Skipping Outbox.", payment.getId());
                return;
            }

            // Only save to outbox if the DB state actually moved to 'newStatus'
            outboxRepository.save(paymentEventFactory.buildEvent(payment, eventType));
        });
    }

    @Override
    public void processPayment(PaymentRequestDTO request) {
        String currentUserId = securityService.getAuthenticatedUserId();
        log.info("Processing payment for Booking ID: {}", request.bookingId());

        BookingResponse verifiedBooking = bookingServiceGateway.getBookingVerification(request.bookingId());

        if (!verifiedBooking.getUserId().equals(currentUserId))
            throw new ForbiddenException();

        BigDecimal amount = new BigDecimal(verifiedBooking.getTotalPrice());

        Payment payment = transactionTemplate.execute(status -> {
            try {
                return paymentRepository.save(new Payment()
                      .setBookingId(request.bookingId())
                      .setUserId(verifiedBooking.getUserId())
                      .setAmount(amount)
                      .setStatus(PENDING));
            } catch (DataIntegrityViolationException ex) {
                throw new PaymentAlreadyProcessedException(request.bookingId());
            }
        });

        Objects.requireNonNull(payment, "Payment entity creation failed unexpectedly");

        try {
            // Simulate Gateway Charge (Stripe/PayPal)
            log.info("Charging {} via payment method {}", verifiedBooking.getTotalPrice(), request.paymentMethodId());

            gatewayService.charge(request.paymentMethodId(), amount);

            finalizePayment(
                  payment,
                  COMPLETED,
                  "PAYMENT_COMPLETED"
            );
        } catch (PaymentDeclinedException e) {
            finalizePayment(
                  payment,
                  FAILED,
                  "PAYMENT_FAILED"
            );
            throw e;

        } catch (Exception e) {
            // We catch everything else (including GatewayTimeoutException)
            // to ensure we DON'T call finalizePayment.
            log.error("Technical failure. State remains PENDING.", e);

            throw new BusinessException(GATEWAY_ERROR);
        }

        log.info("Payment completed. Event emitted for Booking ID: {}", request.bookingId());
    }

    @Transactional
    @Override
    public void handleRefund(RefundRequested refundRequested) {
        UUID bookingID = UUID.fromString(refundRequested.getBookingId());
        log.warn("Saga Compensation triggered for Booking {}. Reason: {}", bookingID, refundRequested.getReason());

        int updatedRows = paymentRepository.atomicRefund(
              bookingID,
              LocalDateTime.now(),
              COMPLETED,
              REFUNDED
        );

        if (updatedRows == 0)
            log.warn("Atomic update failed: Payment not in COMPLETED state or not found.");
    }
}