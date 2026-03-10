package com.eventapp.userservice.exceptions.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.userservice.exceptions.enums.ErrorCode.KEYCLOAK_ERROR;

public class KeycloakUpdateException extends BaseCustomException {

    public KeycloakUpdateException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public KeycloakUpdateException(Throwable cause) {
        super(KEYCLOAK_ERROR, cause, cause.getMessage());
    }
}
