package com.eventapp.sharedutils.exceptions;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {
    private String code;
    private String message;
    private int httpStatus;
    private LocalDateTime timestamp;
    private String path;
    private Set<String> validationErrors;

    public ErrorResponse(IErrorCode errorCode,
                         String message,
                         String path) {
        this.code = errorCode.getCode();
        this.message = message;
        this.httpStatus = errorCode.getHttpStatus();
        this.timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.path = path.replace("uri=", "");
    }
}
