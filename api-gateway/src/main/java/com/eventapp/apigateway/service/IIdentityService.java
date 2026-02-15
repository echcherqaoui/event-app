package com.eventapp.apigateway.service;

import com.eventapp.apigateway.dto.UserPageResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface IIdentityService {

    Mono<UserPageResponse> getPagedUsers(int page, int size);

    /**
     * Assign a role to a user in Keycloak reactively.
     * @param userId Keycloak user ID
     * @param roleName Role to assign
     * @return Mono<Void> completes on success, errors if fails
     */
    Mono<ResponseEntity<String>> assignRole(String userId, String roleName);
}
