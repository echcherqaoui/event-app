package com.eventapp.notificationservice.exception.enums;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements IErrorCode {
    NOTIFICATION_TEMPLATE_ERROR("NOTIF_400", "Template rendering failed for booking ID [%s]. Check syntax.", 400),
    INVALID_SIGNATURE("SEC_403", "Security violation: Invalid HMAC signature for booking ID [%s].", 403),
    SMTP_CONNECTION_FAILED("NOTIF_503", "SMTP server is unreachable. Retrying for message [%s].", 503);
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
