package com.eventapp.paymentservice.exception.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.paymentservice.exception.enums.ErrorCode.PAYMENT_DECLINED;

public class PaymentDeclinedException extends BaseCustomException {

    public PaymentDeclinedException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public PaymentDeclinedException() {
        super(PAYMENT_DECLINED);
    }
}
