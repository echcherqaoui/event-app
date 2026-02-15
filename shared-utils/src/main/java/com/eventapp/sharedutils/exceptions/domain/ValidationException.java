package com.eventapp.sharedutils.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.VALIDATION_FAILED;

public class ValidationException extends BaseCustomException {
    public ValidationException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ValidationException() {
        super(VALIDATION_FAILED);
    }
}