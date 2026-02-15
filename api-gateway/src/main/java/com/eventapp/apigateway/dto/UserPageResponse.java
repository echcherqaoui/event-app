package com.eventapp.apigateway.dto;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public record UserPageResponse(List<UserRepresentation> users,
                               long totalCount,
                               int page,
                               int size) {
}