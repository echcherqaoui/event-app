package com.eventapp.sharedutils.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

public class BusinessException extends BaseCustomException {

    public BusinessException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
