package com.eventapp.bookingservice.client;

import com.eventapp.sharedutils.exceptions.domain.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.GATEWAY_UNAVAILABLE;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventServiceGateway {
    private final EventGrpcClient eventGrpcClient;

    @Retry(name = "event-service")
    @CircuitBreaker(name = "event-service", fallbackMethod = "handlePriceFallback")
    public BigDecimal getEventPrice(Long eventId) {
        return eventGrpcClient.fetchEventPrice(eventId);
    }

    // This method runs when the Circuit is OPEN or the call fails
    @SuppressWarnings("unused")
    protected BigDecimal handlePriceFallback(Long eventId, Throwable t) {
        if (t instanceof CallNotPermittedException)
            log.warn("Circuit is OPEN. Failing fast for Price Fetch on Event ID: {}", eventId);
        else
            log.error("Circuit Breaker triggered. Failed to fetch price for Event ID: {}. Error: {}",
                  eventId, t.getMessage());

        // Since a price is mandatory for a booking, we throw an exception
        // for the RestControllerAdvice to handle as a 503.
        throw new ExternalServiceException(GATEWAY_UNAVAILABLE, "Event Service", eventId, t.getMessage());
    }
}