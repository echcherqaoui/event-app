package com.eventapp.paymentservice.config;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.function.Predicate;

@SuppressWarnings("unused")
public class GrpcFailurePredicate implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable t) {
        if (t instanceof StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();
            
            // These are BUSINESS results, not SERVICE failures.
            // Returning 'false' means: "Don't trip the circuit for this."
            return switch (code) {
                case NOT_FOUND, INVALID_ARGUMENT, PERMISSION_DENIED, ALREADY_EXISTS -> false;
                default -> true; // UNAVAILABLE, DEADLINE_EXCEEDED, etc. = TRIP CIRCUIT
            };
        }

        // If it's a network/JVM error, always record as failure
        return true;
    }
}