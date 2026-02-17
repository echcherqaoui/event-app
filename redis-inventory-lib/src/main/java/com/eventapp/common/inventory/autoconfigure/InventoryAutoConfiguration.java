package com.eventapp.common.inventory.autoconfigure;

import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.common.inventory.ports.impl.DefaultTtlProviderImpl;
import com.eventapp.common.inventory.redis.RedisTicketAdapterImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
@Import({RedisScriptConfig.class, RedisConfig.class})
public class InventoryAutoConfiguration {

    @Bean
    public ITicketCachePort ticketCachePort(StringRedisTemplate redisTemplate,
                                            IInventoryTtlProvider ttlProvider,
                                            @Qualifier("initScript") RedisScript<Long> initScript,
                                            @Qualifier("updateScript")  RedisScript<Long> updateScript,
                                            @Qualifier("cancelScript")  RedisScript<Long> cancelScript,
                                            @Qualifier("deleteScript")  RedisScript<Long> deleteScript,
                                            @Qualifier("checkCapacityScript")  RedisScript<Long> checkCapacityScript,
                                            @Qualifier("reserveScript")  RedisScript<Long> reserveScript,
                                            @Qualifier("confirmScript")  RedisScript<Long> confirmScript,
                                            @Qualifier("idempotentReleaseScript")  RedisScript<Long> idempotentReleaseScript,
                                            @Qualifier("releaseScript")  RedisScript<Long> releaseScript
    ) {

        return new RedisTicketAdapterImpl(
              redisTemplate,
              ttlProvider,
              initScript,
              updateScript,
              cancelScript,
              deleteScript,
              checkCapacityScript,
              reserveScript,
              confirmScript,
              idempotentReleaseScript,
              releaseScript
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public IInventoryTtlProvider ttlProvider() {
        return new DefaultTtlProviderImpl();
    }
}