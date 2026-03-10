package com.eventapp.paymentservice.scheduler;

import com.eventapp.paymentservice.enums.PaymentStatus;
import com.eventapp.paymentservice.model.Payment;
import com.eventapp.paymentservice.repository.PaymentRepository;
import com.eventapp.paymentservice.service.IGatewayService;
import com.eventapp.paymentservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.eventapp.paymentservice.enums.PaymentStatus.COMPLETED;
import static com.eventapp.paymentservice.enums.PaymentStatus.PENDING;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationWorker {

    private final PaymentRepository paymentRepository;
    private final IGatewayService gatewayService;
    private final IPaymentService paymentService;

    // Runs every 10 minutes to avoid system overhead
    @Scheduled(fixedDelayString = "PT10M")
    public void reconcileStuckPayments() {
        // Only look at payments stuck for more than 15 minutes
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        
        List<Payment> stuckPayments = paymentRepository.findByStatusAndCreatedAtBefore(PENDING, cutoff);

        if (stuckPayments.isEmpty()) return;

        log.info("Found {} stuck PENDING payments. Starting reconciliation...", stuckPayments.size());


        for (Payment payment : stuckPayments) {
            try {
                // Query the actual status from Stripe/PayPal/...
                PaymentStatus externalStatus = gatewayService.getRemoteStatus(payment.getBookingId());

                if (externalStatus == COMPLETED)
                    paymentService.finalizePayment(
                          payment,
                          COMPLETED,
                          "PAYMENT_COMPLETED"
                    );
                else if (externalStatus == PaymentStatus.FAILED)
                    paymentService.finalizePayment(
                          payment,
                          PaymentStatus.FAILED,
                          "PAYMENT_FAILED"
                    );

                // If still PENDING on gateway, we leave it for the next run
            } catch (Exception e) {
                log.error("Failed to reconcile payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}