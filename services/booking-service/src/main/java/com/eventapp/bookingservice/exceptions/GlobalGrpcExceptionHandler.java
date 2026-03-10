package com.eventapp.bookingservice.exceptions;

import com.eventapp.bookingservice.exceptions.domain.BookingNotFoundException;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@GrpcAdvice
public class GlobalGrpcExceptionHandler {

    @GrpcExceptionHandler(BookingNotFoundException.class)
    public Status handleEventNotFound(BookingNotFoundException e) {
        log.warn("Booking not found: {}", e.getMessage());
        return Status.NOT_FOUND
                .withDescription(e.getMessage());
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public Status handleInvalidArgument(IllegalArgumentException e) {
        log.warn("Invalid gRPC argument: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription("The request parameters were invalid: " + e.getMessage());
    }

    @GrpcExceptionHandler(AccessDeniedException.class)
    public Status handleAccessDenied(AccessDeniedException e) {
        log.error("Security violation: {}", e.getMessage());
        return Status.PERMISSION_DENIED
              .withDescription("You do not have permission to access this booking.");
    }

    @GrpcExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public Status handleConflict(ObjectOptimisticLockingFailureException e) {
        log.error("Optimistic lock conflict: {}", e.getMessage());
        return Status.ABORTED
              .withDescription("The resource was updated by another transaction. Please retry.");
    }

    @GrpcExceptionHandler(Exception.class)
    public Status handleAny(Exception e) {
        log.error("UNEXPECTED INTERNAL ERROR: ", e);
        return Status.INTERNAL
              .withDescription("An unexpected internal error occurred.");
    }
}