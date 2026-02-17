package com.eventapp.sharedutils.autoconfigure;

import com.eventapp.sharedutils.exceptions.GlobalExceptionHandler;
import com.eventapp.sharedutils.security.ISecurityService;
import com.eventapp.sharedutils.security.keycloak.JwtAuthConverter;
import com.eventapp.sharedutils.security.keycloak.SecurityServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedUtilsAutoConfiguration {
    @Bean
    @ConditionalOnProperty(
          name = "eventapp.sharedutils.exception-handler.enabled",
          havingValue = "true",
          matchIfMissing = true
    )
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public ISecurityService securityService() {
        return new SecurityServiceImpl();
    }

    @Bean
    public JwtAuthConverter jwtAuthConverter() {
        return new JwtAuthConverter();
    }
}
