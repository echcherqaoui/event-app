package com.eventapp.bookingservice.exceptions.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import java.util.UUID;

import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.BOOKING_NOT_FOUND;

public class BookingNotFoundException extends BaseCustomException {

    public BookingNotFoundException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public BookingNotFoundException(UUID bookingId) {
        super(BOOKING_NOT_FOUND, bookingId);
    }
}
