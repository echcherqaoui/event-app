package com.eventapp.common.inventory.ports;

public interface IInventoryTtlProvider {
    long getLockTtlSeconds();

    long getShadowTtlSeconds();

    long getSentinelTtlSeconds();
}
