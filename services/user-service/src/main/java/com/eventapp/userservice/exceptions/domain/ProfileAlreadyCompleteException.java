package com.eventapp.userservice.exceptions.domain;


import com.eventapp.userservice.exceptions.enums.ErrorCode;
import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

public class ProfileAlreadyCompleteException extends BaseCustomException {

    public ProfileAlreadyCompleteException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ProfileAlreadyCompleteException() {
        super(ErrorCode.PROFILE_ALREADY_COMPLETE);
    }
}
