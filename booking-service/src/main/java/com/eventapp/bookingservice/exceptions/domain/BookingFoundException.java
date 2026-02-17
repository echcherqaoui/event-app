package com.eventapp.bookingservice.exceptions.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import java.util.UUID;

import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.BOOKING_NOT_FOUND;

public class BookingFoundException extends BaseCustomException {

    public BookingFoundException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public BookingFoundException(UUID eventId) {
        super(BOOKING_NOT_FOUND, eventId);
    }
}
