package com.eventapp.userservice.service.impl;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.service.IKeycloakIntegrationService;
import com.eventapp.userservice.service.IKeycloakSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeycloakIntegrationService implements IKeycloakIntegrationService {

    private final IKeycloakSyncService keycloakSyncService;

    @Async("keycloakExecutor")
    @Override
    public void syncProfile(String userId, ProfileUpdateRequest profile) {
        //  Call a DIFFERENT service that has the @CircuitBreaker
        keycloakSyncService.updateUserWithRetry(userId, profile);
    }
}