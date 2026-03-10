package com.eventapp.bookingservice.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;

import static com.eventapp.bookingservice.exceptions.enums.ErrorCode.EVENT_SERIALIZATION_ERROR;

public class EventSerializationException extends BaseCustomException {
    
    public EventSerializationException(String eventType, String aggregateId) {
        super(EVENT_SERIALIZATION_ERROR, eventType, aggregateId);
    }

    public EventSerializationException(String eventType, String aggregateId, Throwable cause) {
        super(EVENT_SERIALIZATION_ERROR, eventType, aggregateId);
        this.initCause(cause); 
    }
}