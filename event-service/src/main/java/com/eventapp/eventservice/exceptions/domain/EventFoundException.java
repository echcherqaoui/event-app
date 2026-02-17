package com.eventapp.eventservice.exceptions.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.eventservice.exceptions.enums.ErrorCode.EVENT_NOT_FOUND;


public class EventFoundException extends BaseCustomException {

    public EventFoundException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public EventFoundException(Long eventId) {
        super(EVENT_NOT_FOUND, eventId);
    }
}
