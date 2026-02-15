package com.eventapp.common.inventory.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(IInventoryTtlProvider.class)
public class DefaultTtlProviderImpl implements IInventoryTtlProvider {
    @Override
    public long getLockTtlSeconds() {
        return 600L;
    }

    @Override
    public long getShadowTtlSeconds() {
        return 660L;
    }

    @Override
    public long getSentinelTtlSeconds() {
        return 1800L;
    }
}