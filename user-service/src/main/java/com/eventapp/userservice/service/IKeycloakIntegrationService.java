package com.eventapp.userservice.service;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;

public interface IKeycloakIntegrationService {

    void syncProfile(String userId, ProfileUpdateRequest profile);

}
