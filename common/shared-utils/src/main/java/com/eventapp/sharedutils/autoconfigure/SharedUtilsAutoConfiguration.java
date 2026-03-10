package com.eventapp.sharedutils.autoconfigure;

import com.eventapp.sharedutils.exceptions.GlobalExceptionHandler;
import com.eventapp.sharedutils.security.keycloak.JwtAuthConverter;
import com.eventapp.sharedutils.security.keycloak.SecurityServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedUtilsAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(
          name = "eventapp.sharedutils.exception-handler.enabled",
          havingValue = "true",
          matchIfMissing = true
    )
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Configuration
    @ConditionalOnClass(name = "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")
    public static class SecurityConfiguration {

        @Bean
        public com.eventapp.sharedutils.security.ISecurityService securityService() {
            return new SecurityServiceImpl();
        }

        @Bean
        public com.eventapp.sharedutils.security.keycloak.JwtAuthConverter jwtAuthConverter() {
            return new JwtAuthConverter();
        }
    }
}