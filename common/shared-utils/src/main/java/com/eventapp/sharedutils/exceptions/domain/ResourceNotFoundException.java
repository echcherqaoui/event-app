package com.eventapp.sharedutils.exceptions.domain;

import com.eventapp.sharedutils.exceptions.BaseCustomException;

import static com.eventapp.sharedutils.exceptions.enums.CommonErrorCode.NOT_FOUND;

public class ResourceNotFoundException extends BaseCustomException {

    public ResourceNotFoundException(Object... args) {
        super(NOT_FOUND, args);
    }
}