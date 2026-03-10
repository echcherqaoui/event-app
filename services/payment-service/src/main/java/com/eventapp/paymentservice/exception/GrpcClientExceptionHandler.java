package com.eventapp.paymentservice.exception;

import com.eventapp.sharedutils.exceptions.ErrorResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GrpcClientExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcException(StatusRuntimeException ex, WebRequest request) {
        Status status = ex.getStatus();

        // Map gRPC Status to HTTP status
        int httpStatus = switch (status.getCode()) {
            case NOT_FOUND -> 404;
            case INVALID_ARGUMENT -> 400;
            case PERMISSION_DENIED -> 403;
            case UNAVAILABLE -> 503;
            default -> 500;
        };

        ErrorResponse response = new ErrorResponse(
            status.getCode().name(),
            status.getDescription(), // Message from the remote service
            httpStatus,
            request.getDescription(false)
        );

        return ResponseEntity.status(httpStatus).body(response);
    }
}