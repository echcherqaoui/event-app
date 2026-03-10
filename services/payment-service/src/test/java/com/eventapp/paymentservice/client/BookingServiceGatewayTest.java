package com.eventapp.paymentservice.client;

import com.eventapp.lib.booking.v1.BookingResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Booking Service Gateway Resilience Tests")
class BookingServiceGatewayTest {
    @Autowired private BookingServiceGateway gateway;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoBean private BookingGrpcClient bookingGrpcClient;

    private CircuitBreaker circuitBreaker;
    private static final UUID BOOKING_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("booking-service");
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("Should return response immediately when call succeeds")
    void shouldReturnResponse_WhenCallSucceedsImmediately() {
        BookingResponse mockResponse = BookingResponse.newBuilder()
              .setBookingId(BOOKING_ID.toString())
              .build();

        when(bookingGrpcClient.verifyBooking(BOOKING_ID))
              .thenReturn(mockResponse);

        BookingResponse response = gateway.getBookingVerification(BOOKING_ID);

        assertThat(response.getBookingId()).isEqualTo(BOOKING_ID.toString());
        verify(bookingGrpcClient, times(1)).verifyBooking(BOOKING_ID);
    }

    @Test
    @DisplayName("Should retry 3 times and succeed on the last attempt")
    void shouldRetryAndSucceed_OnTransientFailure() {
        when(bookingGrpcClient.verifyBooking(BOOKING_ID))
              .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE)) // 1st try
              .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE)) // 2nd try
              .thenReturn(BookingResponse.newBuilder().setBookingId(BOOKING_ID.toString()).build()); // 3rd try

        BookingResponse response = gateway.getBookingVerification(BOOKING_ID);

        assertThat(response).isNotNull();
        verify(bookingGrpcClient, times(3)).verifyBooking(BOOKING_ID);
    }

    @Test
    @DisplayName("Should trip circuit after 5 failed sequences")
    void getBookingVerification_ShouldTripCircuit_AfterFiveFailedSequences() {
        // Each gateway call exhausts 3 retries. We need 5 sequences to fail.
        when(bookingGrpcClient.verifyBooking(BOOKING_ID))
              .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> gateway.getBookingVerification(BOOKING_ID))
                  .isInstanceOf(ExternalServiceException.class);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        verify(bookingGrpcClient, times(15)).verifyBooking(BOOKING_ID);
    }

    @Test
    @DisplayName("Should execute fallback immediately when circuit is already open")
    void getBookingVerification_ShouldExecuteFallback_WhenCircuitIsAlreadyOpen() {
        circuitBreaker.transitionToOpenState();

        assertThatThrownBy(() -> gateway.getBookingVerification(BOOKING_ID))
              .isInstanceOf(ExternalServiceException.class);

        // Verify the client was never called because the circuit is open
        verify(bookingGrpcClient, never()).verifyBooking(any());
    }
}