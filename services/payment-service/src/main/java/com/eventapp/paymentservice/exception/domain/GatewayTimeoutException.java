package com.eventapp.paymentservice.exception.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.paymentservice.exception.enums.ErrorCode.GATEWAY_ERROR;

public class GatewayTimeoutException extends BaseCustomException {

    public GatewayTimeoutException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public GatewayTimeoutException() {
        super(GATEWAY_ERROR);
    }
}
