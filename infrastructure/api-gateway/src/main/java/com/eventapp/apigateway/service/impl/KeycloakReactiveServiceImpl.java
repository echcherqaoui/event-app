package com.eventapp.apigateway.service.impl;

import com.eventapp.apigateway.dto.UserPageResponse;
import com.eventapp.apigateway.service.IIdentityService;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class KeycloakReactiveServiceImpl implements IIdentityService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakReactiveServiceImpl.class);

    private final Keycloak keycloak; // injected Keycloak client
    private final String realm;
    private final Mono<Long> cachedUserCount;

    public KeycloakReactiveServiceImpl(Keycloak keycloak,
                                       @Value("${keycloak.admin.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;

        this.cachedUserCount = Mono.fromCallable(() -> this.keycloak.realm(this.realm).users().count().longValue())
              .subscribeOn(Schedulers.boundedElastic())
              .cache(Duration.ofMinutes(2));
    }

    private String promoteUserInKeycloak(String userId, String roleName) {
        RoleRepresentation role = keycloak.realm(realm)
              .roles()
              .get(roleName)
              .toRepresentation();

        keycloak.realm(realm)
              .users()
              .get(userId)
              .roles()
              .realmLevel()
              .add(Collections.singletonList(role));

        return "User promoted successfully";
    }

    @Override
    public Mono<UserPageResponse> getPagedUsers(int page, int size) {
        // flatMap triggers the cachedUserCount
        return cachedUserCount.flatMap(total ->
              Mono.fromCallable(() -> {
                  int first = Math.max(0, page) * size;
                  List<UserRepresentation> users = keycloak.realm(realm).users().list(first, size);

                  return new UserPageResponse(users, total, page, size);
              }).subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<ResponseEntity<String>> assignRole(String userId, String roleName) {
        return Mono.fromCallable(() -> promoteUserInKeycloak(userId, roleName))
              .subscribeOn(Schedulers.boundedElastic())
              .map(ResponseEntity::ok)
              .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
              .onErrorResume(e -> {
                  log.error("Failed to promote user {}: {}", userId, e.getMessage());

                  return Mono.just(ResponseEntity.status(500).body(e.getMessage()));
              });
    }
}
