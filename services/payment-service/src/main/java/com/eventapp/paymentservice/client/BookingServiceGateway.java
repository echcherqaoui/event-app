package com.eventapp.paymentservice.client;

import com.eventapp.lib.booking.v1.BookingResponse;
import com.eventapp.sharedutils.exceptions.domain.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.GATEWAY_UNAVAILABLE;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingServiceGateway {
    private final BookingGrpcClient bookingGrpcClient;

    @Retry(name = "booking-service")
    @CircuitBreaker(name = "booking-service", fallbackMethod = "handleBookingFallback")
    public BookingResponse getBookingVerification(UUID id) {
        return bookingGrpcClient.verifyBooking(id);
    }

    @SuppressWarnings("unused")
    protected BookingResponse handleBookingFallback(UUID id, Throwable t) {
        if (t instanceof CallNotPermittedException)
            log.warn("Circuit is OPEN. Failing fast for Booking ID: {}", id);
         else
            log.error("Circuit Breaker triggered. Verification failed for Booking ID: {}. Error: {}",
                  id, t.getMessage());


        throw new ExternalServiceException(GATEWAY_UNAVAILABLE, "Event Service", id, t.getMessage());
    }
}