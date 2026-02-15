package com.eventapp.common.inventory.config;

public interface IInventoryTtlProvider {
    long getLockTtlSeconds();

    long getShadowTtlSeconds();

    long getSentinelTtlSeconds();
}
