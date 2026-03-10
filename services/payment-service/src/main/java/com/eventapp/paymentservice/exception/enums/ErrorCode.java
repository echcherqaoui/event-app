package com.eventapp.paymentservice.exception.enums;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements IErrorCode {
    PAYMENT_ALREADY_PROCESSED("PAY_409", "Payment for booking %s is already processed or in progress.", 409),
    PAYMENT_DECLINED("PAY_402", "Payment declined: Insufficient funds or invalid card.", 402),
    INVALID_SIGNATURE("SEC_403", "Security violation: Invalid HMAC signature for payment ID [%s].", 403),
    EVENT_SERIALIZATION_ERROR("EVT_500", "Critical error serializing event [%s] for ID [%s].", 500),
    GATEWAY_ERROR("PAY_502", "Payment gateway unreachable or timed out. Status unknown.", 502);

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
