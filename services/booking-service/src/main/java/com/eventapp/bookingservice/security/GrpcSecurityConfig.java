package com.eventapp.bookingservice.security;

import com.eventapp.sharedutils.security.keycloak.JwtAuthConverter;
import net.devh.boot.grpc.server.security.authentication.BearerAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.check.AccessPredicate;
import net.devh.boot.grpc.server.security.check.GrpcSecurityMetadataSource;
import net.devh.boot.grpc.server.security.check.ManualGrpcSecurityMetadataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@Configuration
@EnableMethodSecurity(proxyTargetClass = true)
public class GrpcSecurityConfig {

    // Tells the library HOW to read the token from headers
    @Bean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        // Extracts the raw "Bearer <token>" string from the gRPC metadata (headers).
        return new BearerAuthenticationReader(BearerTokenAuthenticationToken::new);
    }

    @Bean("grpcAuthenticationManager")
    public AuthenticationManager authenticationManager(JwtDecoder jwtDecoder,
                                                       JwtAuthConverter jwtAuthConverter) {
        // Validates the JWT via Keycloak and converts claims into Spring Security authorities using custom converter.
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(jwtAuthConverter);

        return new ProviderManager(provider);
    }

    @Bean
    public GrpcSecurityMetadataSource grpcSecurityMetadataSource() {
        // Acts as a global security guard that denies any unauthenticated gRPC call by default.
        ManualGrpcSecurityMetadataSource source = new ManualGrpcSecurityMetadataSource();

        source.setDefault(AccessPredicate.denyAll());

        return source;
    }
}