package com.eventapp.bookingservice.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.INVALID_SIGNATURE;

public class EventSecurityException extends BaseCustomException {
    public EventSecurityException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public EventSecurityException(String bookingId) {
        super(INVALID_SIGNATURE, bookingId);
    }
}