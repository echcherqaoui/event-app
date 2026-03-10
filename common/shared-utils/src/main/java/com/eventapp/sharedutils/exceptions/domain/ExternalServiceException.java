package com.eventapp.sharedutils.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.EXTERNAL_SERVICE_ERROR;

public class ExternalServiceException extends BaseCustomException {

    public ExternalServiceException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ExternalServiceException(Object... args) {
        super(EXTERNAL_SERVICE_ERROR, args);
    }
}