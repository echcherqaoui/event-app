package com.eventapp.bookingservice.client;


import com.eventapp.sharedutils.exceptions.domain.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Event Service Gateway Resilience and Fault Tolerance")
class EventServiceGatewayTest {

    @Autowired private EventServiceGateway gateway;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoBean private EventGrpcClient eventGrpcClient;

    private CircuitBreaker circuitBreaker;
    private static final Long EVENT_ID = 500L;
    private static final BigDecimal MOCK_PRICE = new BigDecimal("150.00");

    @BeforeEach
    void setUp() {
        // Use consistent instance name as defined in YAML
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("event-service");
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("Should return price directly when the first gRPC call is successful")
    void shouldReturnPrice_WhenCallSucceedsImmediately() {
        when(eventGrpcClient.fetchEventPrice(EVENT_ID)).thenReturn(MOCK_PRICE);

        BigDecimal result = gateway.getEventPrice(EVENT_ID);

        assertThat(result).isEqualByComparingTo(MOCK_PRICE);
        verify(eventGrpcClient, times(1)).fetchEventPrice(EVENT_ID);
    }

    @Test
    @DisplayName("Should retry 3 times and succeed on the last attempt")
    void shouldRetryAndSucceed_OnTransientFailure() {
        when(eventGrpcClient.fetchEventPrice(EVENT_ID))
              .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE))
              .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE))
              .thenReturn(MOCK_PRICE);

        BigDecimal result = gateway.getEventPrice(EVENT_ID);

        assertThat(result).isEqualByComparingTo(MOCK_PRICE);
        verify(eventGrpcClient, times(3)).fetchEventPrice(EVENT_ID);
    }

    @Test
    @DisplayName("Should trip circuit after 5 failed sequences")
    void shouldTripCircuit_AfterFiveFailedSequences() {
        when(eventGrpcClient.fetchEventPrice(EVENT_ID))
              .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> gateway.getEventPrice(EVENT_ID))
                  .isInstanceOf(ExternalServiceException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        verify(eventGrpcClient, times(15)).fetchEventPrice(EVENT_ID);
    }

    @Test
    @DisplayName("Should execute fallback immediately when circuit is already open")
    void shouldExecuteFallback_WhenCircuitIsAlreadyOpen() {
        circuitBreaker.transitionToOpenState();

        assertThatThrownBy(() -> gateway.getEventPrice(EVENT_ID))
              .isInstanceOf(ExternalServiceException.class);

        // Verify the client was never called because the circuit is open
        verify(eventGrpcClient, never()).fetchEventPrice(anyLong());
    }
}