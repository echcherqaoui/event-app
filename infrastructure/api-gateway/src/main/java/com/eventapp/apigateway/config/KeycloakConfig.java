package com.eventapp.apigateway.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakConfig(@Value("${keycloak.admin.server-url}") String serverUrl,
                          @Value("${keycloak.admin.realm}") String realm,
                          @Value("${keycloak.admin.client-id}") String clientId,
                          @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
              .serverUrl(serverUrl)
              .realm(realm)
              .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
              .clientId(clientId)
              .clientSecret(clientSecret)
              .build();
    }
}
