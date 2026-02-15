package com.eventapp.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
@EnableRedisWebSession
public class RedisSessionConfig {
    private final String cookieName;

    public RedisSessionConfig(@Value("${server.reactive.session.cookie.name}") String cookieName) {
        this.cookieName = cookieName;
    }

    // Tells Spring HOW to send the session ID to the browser
    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName(cookieName);
        resolver.addCookieInitializer(builder -> builder
              .path("/")
              .sameSite("Lax")
              .secure(false) // Set to true in production with HTTPS
              .httpOnly(true)
        );
        return resolver;
    }

    // This implementation saves the tokens INSIDE the WebSession
    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }
}
