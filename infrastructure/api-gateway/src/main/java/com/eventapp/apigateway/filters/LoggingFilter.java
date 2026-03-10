package com.eventapp.apigateway.filters;

import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * Global filter that logs HTTP method, path, status code, and duration for every request.
 */
@Component
public class LoggingFilter implements GlobalFilter {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();

        long startTime = System.currentTimeMillis();

        // Log after response is sent
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = 0;

            if (exchange.getResponse().getStatusCode() != null)
                statusCode = exchange.getResponse().getStatusCode().value();

            log.info(
                  "Response sent: {} {} | Status: {} | Duration: {}ms",
                  method,
                  path,
                  statusCode,
                  duration
            );
        }));
    }
}