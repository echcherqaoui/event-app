package com.eventapp.common.inventory.config;

import com.eventapp.common.inventory.ports.IInventoryTtlProvider;

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