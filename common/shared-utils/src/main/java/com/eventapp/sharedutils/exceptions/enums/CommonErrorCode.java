package com.eventapp.sharedutils.exceptions.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements IErrorCode{

    VALIDATION_FAILED("REQ_400", "Validation failed: %s", 400),

    INVALID_TOKEN_CLAIMS("AUTH_400", "JWT missing or invalid claim: %s", 400),

    UNAUTHORIZED("AUTH_401", "Unauthorized: Token missing or invalid", 401),

    ACCESS_DENIED("GEN_403", "Operation not allowed", 403),

    NOT_FOUND("RES_404", "%s with ID [%s] was not found.", 404),

    INTERNAL_SERVER_ERROR("GEN_500", "Internal server error [Ref: %s]", 500),

    GATEWAY_UNAVAILABLE("RES_503", "Service [%s] unavailable for ID [%s]. Reason: %s", 503),

    EXTERNAL_SERVICE_ERROR("SVC_503", "External service [%s] is unavailable or returned an error: %s", 503);

    private final String code;
    private final String message;
    private final int httpStatus;

    @Override
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0)
            return getMessage();

        return String.format(getMessage(), args);
    }
}
