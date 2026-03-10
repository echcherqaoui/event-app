package com.eventapp.eventservice.service.impl;

import com.eventapp.common.inventory.domain.EventLifecycleStatus;
import com.eventapp.common.inventory.ports.ITicketCachePort;
import com.eventapp.eventservice.dto.request.EventRequestDTO;
import com.eventapp.eventservice.dto.response.EventResponseDTO;
import com.eventapp.eventservice.events.EventCanceledEvent;
import com.eventapp.eventservice.events.EventCreatedEvent;
import com.eventapp.eventservice.events.EventDeletedEvent;
import com.eventapp.eventservice.events.EventUpdatedEvent;
import com.eventapp.eventservice.exceptions.domain.EventNotFoundException;
import com.eventapp.eventservice.model.Event;
import com.eventapp.eventservice.repository.EventRepository;
import com.eventapp.eventservice.service.IEventService;
import com.eventapp.sharedutils.exceptions.domain.ForbiddenException;
import com.eventapp.sharedutils.security.ISecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

import static com.eventapp.eventservice.enums.EventStatus.CANCELLED;
import static com.eventapp.eventservice.enums.EventStatus.PUBLISHED;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements IEventService {

    private final EventRepository eventRepository;
    private final ISecurityService securityService;
    private final ITicketCachePort inventoryService;

    private final ApplicationEventPublisher eventPublisher;

    private Event getEventById(Long id){
        return eventRepository.findByIdAndStatusNot(id, CANCELLED)
              .orElseThrow(() -> new EventNotFoundException(id));
    }

    private void checkOwnership(Event event) {
        String currentUserId = securityService.getAuthenticatedUserId();

        if (!event.getOrganizerId().equals(currentUserId))
            throw new ForbiddenException();
    }

    @Transactional
    @Override
    public EventResponseDTO createEvent(EventRequestDTO eventRequest) {
        String currentUserId = securityService.getAuthenticatedUserId();

        Event event = new Event()
              .setTitle(eventRequest.title())
              .setDescription(eventRequest.description())
              .setEventDate(eventRequest.eventDate())
              .setLocation(eventRequest.location())
              .setPrice(eventRequest.price())
              .setCapacity(eventRequest.capacity())
              .setOrganizerId(currentUserId);

        Event savedEvent = eventRepository.save(event);

        /*
         *  Publishes an internal event to sync the state with Redis.
         *  IMPORTANT:  (Phase = AFTER_COMMIT). This ensures Redis is only populated if the database
         *  transaction succeeds, preventing "ghost inventory" for failed event creations.
         */
        eventPublisher.publishEvent(
              new EventCreatedEvent(
                    savedEvent.getId(),
                    savedEvent.getCapacity(),
                    savedEvent.getStatus()
              )
        );

        return EventResponseDTO.from(savedEvent);
    }

    @Override
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
         return eventRepository.findAllByStatusNot(CANCELLED, pageable)
               .map(EventResponseDTO::from);
    }

    @Transactional(readOnly = true)
    @Override
    public EventResponseDTO getEventId(Long id) {
        return EventResponseDTO.from(
              getEventById(id)
        );
    }

    @Transactional
    @Override
    public EventResponseDTO updateEvent(Long eventId, EventRequestDTO eventRequest) {
        inventoryService.validateCapacityChange(
              eventId,
              eventRequest.capacity(),
              EventLifecycleStatus.CANCELLED
        );

        Event event = getEventById(eventId);

        // Security: Ownership Check
        checkOwnership(event);

        boolean isCapacityChanged = !Objects.equals(eventRequest.capacity(), event.getCapacity());

        event.setCapacity(eventRequest.capacity())
              .setTitle(eventRequest.title())
              .setEventDate(eventRequest.eventDate())
              .setLocation(eventRequest.location())
              .setPrice(eventRequest.price());

        Event updatedEvent = eventRepository.save(event);

        if (isCapacityChanged) {
            /*
             * Publishes an event handled by a synchronous @EventListener.
             * * IMPORTANT: Any exception thrown by the Redis script (e.g., CAPACITY_TOO_LOW)
             * will propagate here and force a ROLLBACK of the database transaction, ensuring data integrity.
             */
            eventPublisher.publishEvent(
                  new EventUpdatedEvent(
                        updatedEvent.getId(),
                        updatedEvent.getCapacity(),
                        CANCELLED
                  )
            );
        }

        // Reaching this point guarantees Redis has accepted the change.
        // The method exits, and Spring commits the DB transaction.
        return EventResponseDTO.from(updatedEvent);
    }

    @Override
    public Page<EventResponseDTO> getMyEvents(Pageable pageable) {
        String currentUserId = securityService.getAuthenticatedUserId();

        return eventRepository.findByOrganizerId(currentUserId, pageable)
              .map(EventResponseDTO::from);
    }

    @Override
    public Page<EventResponseDTO> getActiveEvents(Pageable pageable) {
        return eventRepository.findActiveEvents(PUBLISHED, pageable)
              .map(EventResponseDTO::from);
    }

    @Override
    public BigDecimal getPriceById(Long id){
        return eventRepository.findPriceByIdAndStatusNot(id, CANCELLED)
              .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    @Override
    public void cancelEvent(Long eventId) {
        Event event = getEventById(eventId);
        checkOwnership(event);

        // Commit to Source of Truth first to ensure legal state persistence.
        event.setStatus(CANCELLED);
        eventRepository.save(event);

        // Notify Redis/Downstream. If this fails, the DB is still correct.
        eventPublisher.publishEvent(
              new EventCanceledEvent(
                    eventId
              )
        );
    }

    @Transactional
    @Override
    public void requestDeleteEvent(Long eventId) {
        // This will call the listener and WAIT for it to finish
        // If the listener throws an exception, the code below NEVER runs.
        eventPublisher.publishEvent(
              new EventDeletedEvent(
                    eventId,
                    CANCELLED
              )
        );

        // If we reached here, it means Redis is clean. Safe to delete.
        eventRepository.deleteById(eventId);
        log.info("Full deletion successful for event {}", eventId);
    }
}