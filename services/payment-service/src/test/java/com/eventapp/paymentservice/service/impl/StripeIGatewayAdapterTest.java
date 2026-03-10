package com.eventapp.paymentservice.service.impl;

import com.eventapp.paymentservice.enums.PaymentStatus;
import com.eventapp.paymentservice.exception.domain.GatewayTimeoutException;
import com.eventapp.paymentservice.exception.domain.PaymentDeclinedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Stripe Gateway Adapter Unit Tests")
class StripeIGatewayAdapterTest {

    private StripeIGatewayAdapter adapter;

    @Nested
    @DisplayName("Charge Method - Deterministic Inputs")
    class ChargeInputTests {

        @Test
        @DisplayName("Should throw PaymentDeclinedException when ID is CARD_DECLINED")
        void shouldDecline_WhenSpecificIdProvided() {
            adapter = new StripeIGatewayAdapter(0.0);

            assertThatThrownBy(() -> adapter.charge("CARD_DECLINED", BigDecimal.TEN))
                  .isInstanceOf(PaymentDeclinedException.class);
        }

        @Test
        @DisplayName("Should throw GatewayTimeoutException when ID is GATEWAY_TIMEOUT")
        void shouldTimeout_WhenSpecificIdProvided() {
            adapter = new StripeIGatewayAdapter(0.0);

            assertThatThrownBy(() -> adapter.charge("GATEWAY_TIMEOUT", BigDecimal.TEN))
                  .isInstanceOf(GatewayTimeoutException.class);
        }
    }

    @Nested
    @DisplayName("Charge Method - Simulation Logic")
    class ChargeSimulationTests {

        @Test
        @DisplayName("Should eventually fail")
        void shouldFail_WhenFailureRateIsOne() {
            adapter = new StripeIGatewayAdapter(1.0);

            assertThatThrownBy(() -> adapter.charge("any_id", BigDecimal.TEN))
                  .isInstanceOfAny(GatewayTimeoutException.class, PaymentDeclinedException.class);
        }
    }

    @Nested
    @DisplayName("Remote Status Method")
    class RemoteStatusTests {
        @Test
        @DisplayName("Should return a valid PaymentStatus")
        void shouldReturnNonNullStatus() {
            adapter = new StripeIGatewayAdapter(0.0);

            PaymentStatus status = adapter.getRemoteStatus(UUID.randomUUID());

            assertThat(status).isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
        }
    }
}