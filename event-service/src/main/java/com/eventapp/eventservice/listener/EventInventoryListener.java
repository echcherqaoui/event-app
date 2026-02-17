package com.eventapp.eventservice.listener;

import com.eventapp.common.inventory.domain.EventLifecycleStatus;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.eventservice.events.EventCanceledEvent;
import com.eventapp.eventservice.events.EventCreatedEvent;
import com.eventapp.eventservice.events.EventDeletedEvent;
import com.eventapp.eventservice.events.EventUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EventInventoryListener {

    private final ITicketCachePort ticketCachePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCreated(EventCreatedEvent event) {
        ticketCachePort.initializeInventory(
              event.eventId(),
              event.capacity(),
              EventLifecycleStatus.PUBLISHED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventUpdated(EventUpdatedEvent event) {
        ticketCachePort.updateInventory(
              event.eventId(),
              event.newCapacity(),
              EventLifecycleStatus.CANCELLED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCanceled(EventCanceledEvent event) {
        ticketCachePort.cancelInventory(
              event.eventId(),
              EventLifecycleStatus.CANCELLED
        );
    }

    @EventListener
    public void onEventDeleted(EventDeletedEvent event) {
        ticketCachePort.deleteInventory(
              event.eventId()
        );
    }
}
