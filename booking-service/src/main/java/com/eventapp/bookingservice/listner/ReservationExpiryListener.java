package com.eventapp.bookingservice.listner;

import com.eventapp.bookingservice.service.IBookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class ReservationExpiryListener extends KeyExpirationEventMessageListener {

    private final IBookingService bookingService;

    public ReservationExpiryListener(RedisMessageListenerContainer container, IBookingService bookingService) {
        super(container);
        this.bookingService = bookingService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString(); // Format: res:lock:{bookingId}
        if (!expiredKey.startsWith("res:lock:"))
            return;

        String[] parts = expiredKey.split(":");

        if (parts.length != 3) {
            log.warn("Invalid expired key format: {}", expiredKey);
            return;
        }

        try {
            UUID bookingId = UUID.fromString(parts[2]);
            bookingService.handleAutomaticExpiration(bookingId);
        } catch (Exception e) {
            log.error("Failed to process expired reservation key: {}", expiredKey, e);
        }
    }
}