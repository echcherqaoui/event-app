package com.eventapp.commonsecurity.autoconfigure;

import com.eventapp.commonsecurity.service.IEventAuthenticator;
import com.eventapp.commonsecurity.service.impl.HmacService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class EventSecurityAutoConfiguration {

    @Bean
    public IEventAuthenticator eventAuthenticator(@Value("${app.security.hmac-secret}") String secret) {
        return new HmacService(secret);
    }
}