package com.eventapp.bookingservice.config;

import com.eventapp.common.inventory.ports.IInventoryTtlProvider;
import com.eventapp.common.inventory.domain.ReservationSettings;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Setter
@Configuration
@ConfigurationProperties(prefix = "app.inventory")
public class BookingTtlProvider implements IInventoryTtlProvider {
    private long lockMin;
    private long shadowBufferMin;
    private long sentinelMin;
    private ReservationSettings settings;

    @PostConstruct
    public void init() {
        this.settings = new ReservationSettings(
              lockMin,
              shadowBufferMin,
              sentinelMin
        );
    }

    @Override
    public long getLockTtlSeconds() {
        return settings.lockTtlSeconds();
    }

    @Override
    public long getShadowTtlSeconds() {
        return settings.shadowTtlSeconds();
    }

    @Override
    public long getSentinelTtlSeconds() {
        return settings.sentinelTtlSeconds();
    }
}