package com.eventapp.userservice.service;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.exceptions.domain.KeycloakUpdateException;

public interface IKeycloakSyncService {

    void updateUserWithRetry(String userId, ProfileUpdateRequest profile) throws KeycloakUpdateException;

}
