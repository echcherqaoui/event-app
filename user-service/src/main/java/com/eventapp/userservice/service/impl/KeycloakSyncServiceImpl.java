package com.eventapp.userservice.service.impl;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.exceptions.domain.KeycloakUpdateException;
import com.eventapp.userservice.service.IKeycloakSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakSyncServiceImpl implements IKeycloakSyncService {

    private final Keycloak keycloak;
    @Value("${keycloak.admin.realm}") private String realm;

    private void updateUserInKeycloak(String userId, ProfileUpdateRequest request) {
        try {
            UserResource userResource = keycloak
                  .realm(realm)
                  .users()
                  .get(userId);

            UserRepresentation user = new UserRepresentation();

            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());

            userResource.update(user);

        } catch (Exception ex) {
            throw new KeycloakUpdateException(ex);
        }
    }

    @Retryable(
          maxAttempts = 4,
          backoff = @Backoff(delay = 2000, multiplier = 2),
          retryFor = KeycloakUpdateException.class
    )
    @Override
    public void updateUserWithRetry(String userId, ProfileUpdateRequest profile) throws KeycloakUpdateException {
        log.info("Starting async sync to Keycloak for user: {}", userId);
        updateUserInKeycloak(userId, profile);
    }

    @Recover
    public void handleSyncFailure(KeycloakUpdateException e, String userId, ProfileUpdateRequest profile) {
        log.error("❌ Keycloak sync failed for user {} after 4 attempts. Manual intervention required.", userId);
    }
}