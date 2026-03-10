package com.eventapp.notificationservice.exception.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

public class NotificationException extends BaseCustomException {
    public NotificationException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}