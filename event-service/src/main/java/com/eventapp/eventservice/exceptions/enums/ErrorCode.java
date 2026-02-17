package com.eventapp.eventservice.exceptions.enums;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements IErrorCode {
    CAPACITY_CONFLICT_RESERVED("EVT_409_RESERVED", "Cannot lower capacity to %d. %d tickets are sold and %d are currently being purchased.", 409),

    EVENT_NOT_FOUND("EVENT_404", "Event with ID %s not found", 404);

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
