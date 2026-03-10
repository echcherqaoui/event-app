package com.eventapp.userservice.service.impl;

import com.eventapp.userservice.dto.request.ProfileUpdateRequest;
import com.eventapp.userservice.service.IKeycloakSyncService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.eventapp.userservice.enums.Gender.MALE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class KeycloakSyncServiceResilienceTest {

    @Autowired private IKeycloakSyncService syncService;

    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean private Keycloak keycloak;

    // Mocks for Keycloak's fluent API
    private UserResource userResource;
    private static final String USER_ID = "user-123";
    private final ProfileUpdateRequest request = new ProfileUpdateRequest(
          "Ahmed",
          "EDER",
          MALE,
          null,
          null,
          null
    );

    @BeforeEach
    void setup() {
        // Reset Circuit Breaker state between tests
        circuitBreakerRegistry.circuitBreaker("keycloakSync").reset();

        // Set up the fluent API chain: keycloak.realm().users().get()
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        userResource = mock(UserResource.class);

        when(keycloak.realm(anyString()))
              .thenReturn(realmResource);

        when(realmResource.users())
              .thenReturn(usersResource);

        when(usersResource.get(anyString()))
              .thenReturn(userResource);
    }

    @Test
    @DisplayName("Should update Keycloak user directly when the first attempt succeeds")
    void shouldUpdateKeycloakUser_WhenRequestIsSuccessful() {
        when(userResource.toRepresentation())
              .thenReturn(new UserRepresentation());

        syncService.updateUserWithRetry(USER_ID, request);

        verify(userResource, times(1)).update(any());
    }

    @Test
    @DisplayName("Should abort sync immediately and skip retries when Keycloak returns 404")
    void shouldAbortSyncWithoutRetry_WhenKeycloakReturns404NotFound() {
        // Create a 404 WebApplicationException
        Response response = Response.status(404).build();
        WebApplicationException ex = new WebApplicationException(response);
        
        when(userResource.toRepresentation())
              .thenThrow(ex);

        syncService.updateUserWithRetry(USER_ID, request);

        // Verify it only tried ONCE
        verify(userResource, times(1)).toRepresentation();
        verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("Should retry the operation and eventually succeed when encountering a transient failure")
    void shouldRetryAndSucceed_WhenFirstAttemptEncountersTransientFailure() {
        when(userResource.toRepresentation())
                .thenThrow(new RuntimeException("Temporary Timeout")) // Attempt 1
                .thenReturn(new UserRepresentation());                // Attempt 2 (Success)

        syncService.updateUserWithRetry(USER_ID, request);

        verify(userResource, times(2)).toRepresentation();
    }

    @Test
    @DisplayName("Should exhaust all retries and transition Circuit Breaker to OPEN state after persistent failures")
    void shouldExhaustRetriesAndOpenCircuit_WhenFailuresPersist() {
        // Keycloak consistently throws retryable exceptions
        when(userResource.toRepresentation())
              .thenThrow(new RuntimeException("Fatal Error"));

        // We make enough calls to exceed the 'minimumNumberOfCalls' (5)
        for (int i = 0; i < 5; i++)
            syncService.updateUserWithRetry(USER_ID, request);

        // Verify the userResource was actually hit for the first 5 logic cycles
        // (5 cycles * 4 retries = 20 attempts total)
        verify(userResource, times(20)).toRepresentation();

        // The 6th call should be blocked immediately by the Circuit Breaker
        syncService.updateUserWithRetry(USER_ID, request);

        // The count remains 20. The 6th call never reached the method body.
        verify(userResource, times(20)).toRepresentation();
    }
}