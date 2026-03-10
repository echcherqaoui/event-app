package com.eventapp.userservice.service.impl;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.exceptions.domain.KeycloakUpdateException;
import com.eventapp.userservice.service.IKeycloakSyncService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KeycloakSyncServiceImpl implements IKeycloakSyncService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakSyncServiceImpl(Keycloak keycloak,
                                   @Value("${keycloak.admin.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    @CircuitBreaker(name = "keycloakSync", fallbackMethod = "handleSyncFailure")
    @Retry(name = "keycloakSync")
    public void updateUserWithRetry(String userId, ProfileUpdateRequest profile) throws KeycloakUpdateException {
        log.info("Starting async sync to Keycloak for user: {}", userId);

        try {
            UserResource userResource = keycloak
                  .realm(realm)
                  .users()
                  .get(userId);

            UserRepresentation user = userResource.toRepresentation();

            user.setFirstName(profile.firstName());
            user.setLastName(profile.lastName());

            userResource.update(user);

        } catch (WebApplicationException ex) {
            int status = ex.getResponse().getStatus();

            if (status == 404) {
                log.error("Critical: User {} not found in Keycloak. Aborting sync.", userId);
                return;
            }

            log.error("Keycloak API error: Status {}", status);
            throw new KeycloakUpdateException(ex);
        } catch (Exception ex) {
            throw new KeycloakUpdateException(ex);
        }
    }

    @SuppressWarnings("unused")
    public void handleSyncFailure(String userId, ProfileUpdateRequest profile, Throwable t) {
        if (t instanceof CallNotPermittedException)
            log.error("Keycloak Sync BLOCKED: Circuit is OPEN for user {}", userId);
        else
            log.error("Keycloak Sync FAILED after retries for user {}: {}", userId, t.getMessage());
    }
}