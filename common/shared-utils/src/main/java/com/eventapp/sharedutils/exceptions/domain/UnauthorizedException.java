package com.eventapp.sharedutils.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.UNAUTHORIZED;

public class UnauthorizedException extends BaseCustomException {
    public UnauthorizedException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public UnauthorizedException() {
        super(UNAUTHORIZED);
    }
}