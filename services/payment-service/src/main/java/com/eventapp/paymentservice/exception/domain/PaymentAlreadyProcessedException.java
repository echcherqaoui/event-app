package com.eventapp.paymentservice.exception.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import java.util.UUID;

import static com.eventapp.paymentservice.exception.enums.ErrorCode.PAYMENT_ALREADY_PROCESSED;

public class PaymentAlreadyProcessedException extends BaseCustomException {

    public PaymentAlreadyProcessedException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public PaymentAlreadyProcessedException(UUID bookingId) {
        super(PAYMENT_ALREADY_PROCESSED, bookingId);
    }
}
