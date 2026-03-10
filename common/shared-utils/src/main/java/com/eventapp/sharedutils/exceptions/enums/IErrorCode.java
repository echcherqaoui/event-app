package com.eventapp.sharedutils.exceptions.enums;

public interface IErrorCode {
    String getCode();
    String getMessage();
    int getHttpStatus();

    String formatMessage(Object... args);
}