package com.eventapp.eventservice.security;

import com.eventapp.sharedutils.security.keycloak.JwtAuthConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;
    @Value("${api.base-path}") private String basePath;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String eventPath = basePath + "/events";

        http
              .csrf(AbstractHttpConfigurer::disable)
              .authorizeHttpRequests(auth -> auth
                    // Public GET endpoints
                    .requestMatchers(HttpMethod.GET, eventPath, eventPath + "/active", eventPath + "/{id}").permitAll()

                    // Organizer/Admin Access (Updates & Cancellation)
                    .requestMatchers(HttpMethod.POST, eventPath).hasAnyRole("ORGANIZER", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, eventPath + "/*/cancel").hasAnyRole("ORGANIZER", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, eventPath + "/*").hasAnyRole("ORGANIZER", "ADMIN")

                    //  Admin Only Access (Hard Deletion)
                    .requestMatchers(HttpMethod.DELETE, eventPath + "/*").hasRole("ADMIN")

                    .requestMatchers(eventPath + "/me").authenticated()
                    .anyRequest().authenticated()
              )
              .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
              );

        return http.build();
    }
}