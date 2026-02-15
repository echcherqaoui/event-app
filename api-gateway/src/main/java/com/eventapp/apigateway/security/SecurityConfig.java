package com.eventapp.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
              .csrf(ServerHttpSecurity.CsrfSpec::disable)
              .authorizeExchange(exchanges -> exchanges
                    .pathMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                    .pathMatchers("/api/v1/events/me").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/v1/events").authenticated()

                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyExchange().authenticated()
              ).oauth2Login(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

        return userRequest -> delegate.loadUser(userRequest).map(oidcUser -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());
            Map<String, Object> realmAccess = oidcUser.getAttribute("realm_access");

            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles)
                roles.forEach(role -> mappedAuthorities.add(
                      new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
                );

            return new DefaultOidcUser(
                  mappedAuthorities,
                  oidcUser.getIdToken(),
                  oidcUser.getUserInfo()
            );
        });
    }

}
