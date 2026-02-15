package com.eventapp.userservice.exceptions.domain;


import com.eventapp.sharedutils.exceptions.BaseCustomException;
import com.eventapp.sharedutils.exceptions.enums.IErrorCode;

import static com.eventapp.userservice.exceptions.enums.ErrorCode.USER_NOT_FOUND;

public class UserNotFoundException extends BaseCustomException {

    public UserNotFoundException(IErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public UserNotFoundException(String userId) {
        super(USER_NOT_FOUND, userId);
    }
}
