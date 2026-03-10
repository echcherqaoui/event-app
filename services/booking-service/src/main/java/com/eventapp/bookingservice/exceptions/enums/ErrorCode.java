package com.eventapp.bookingservice.exceptions.enums;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements IErrorCode {
    BOOKING_NOT_FOUND("BOOKING_404", "booking with ID %s not found", 404),
    RESERVATION_INVALID("BOK_400", "Reservation for event %s is invalid, expired, or has changed status.", 410),
    INVALID_SIGNATURE("SEC_403", "Security violation: Invalid HMAC signature for booking ID [%s].", 403),
    EVENT_SERIALIZATION_ERROR("EVT_500", "Critical error serializing event [%s] for ID [%s].", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    @Override
    public String formatMessage(Object... args) {
        if (args == null || args.length == 0)
            return getMessage();

        return String.format(getMessage(), args);
    }
}
