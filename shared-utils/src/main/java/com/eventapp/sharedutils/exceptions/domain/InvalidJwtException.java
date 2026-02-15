package com.eventapp.sharedutils.exceptions.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.INVALID_TOKEN_CLAIMS;


public class InvalidJwtException extends BaseCustomException {

    public InvalidJwtException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public InvalidJwtException(String claimName) {
        super(INVALID_TOKEN_CLAIMS, claimName);
    }
}
