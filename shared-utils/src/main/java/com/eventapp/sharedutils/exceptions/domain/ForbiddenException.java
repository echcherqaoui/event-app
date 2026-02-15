package com.eventapp.sharedutils.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.ACCESS_DENIED;

public class ForbiddenException extends BaseCustomException {
    public ForbiddenException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ForbiddenException() {
        super(ACCESS_DENIED);
    }
}