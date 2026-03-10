package com.eventapp.common.inventory.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<Long> initScript() {
        return loadScript("scripts/init_inventory.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> updateScript() {
        return loadScript("scripts/update_event_capacity.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> cancelScript() {
        return loadScript("scripts/cancel_event.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> deleteScript() {
        return loadScript("scripts/delete_event.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> checkCapacityScript() {
        return loadScript("scripts/check_remaining_capacity.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> reserveScript() {
        return loadScript("scripts/reserve_ticket.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> confirmScript() {
        return loadScript("scripts/confirm_purchase.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> idempotentReleaseScript() {
        return loadScript("scripts/idempotent_release.lua", Long.class);
    }

    @Bean
    public RedisScript<Long> releaseScript() {
        return loadScript("scripts/release_reservation.lua", Long.class);
    }


    private <T> RedisScript<T> loadScript(String path, Class<T> type) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();

        script.setLocation(new ClassPathResource(path));
        script.setResultType(type);
        return script;
    }
}