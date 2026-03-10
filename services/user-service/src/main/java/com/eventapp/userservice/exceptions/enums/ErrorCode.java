package com.eventapp.userservice.exceptions.enums;

import com.eventapp.sharedutils.exceptions.enums.IErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements IErrorCode {

    // ===== User Service =====
    USER_NOT_FOUND("USER_404", "User with id %s not found", 404),
    PROFILE_ALREADY_COMPLETE("USER_409", "Profile is already complete", 409),

    // ===== Keycloak =====
    KEYCLOAK_ERROR("KEYCLOAK_500", "Failed to update Keycloak: %s", 500);


    private final String code;
    private final String message;
    private final int httpStatus;

    public String formatMessage(Object... args) {
        return String.format(message, args);
    }
}
