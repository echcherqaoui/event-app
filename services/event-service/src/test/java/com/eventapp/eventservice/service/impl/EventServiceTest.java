package com.eventapp.eventservice.service.impl;

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
import com.eventapp.sharedutils.exceptions.domain.ForbiddenException;
import com.eventapp.sharedutils.security.ISecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static com.eventapp.eventservice.enums.EventStatus.CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Service Functional Tests")
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private ISecurityService securityService;
    @Mock private ITicketCachePort inventoryService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private EventServiceImpl eventService;

    private static final String ORGANIZER_ID = "user-123";
    private static final Long EVENT_ID = 1L;
    private Event existingEvent;
    private EventRequestDTO request;

    @BeforeEach
    void setUp() {
        existingEvent = new Event()
              .setId(EVENT_ID)
              .setOrganizerId(ORGANIZER_ID)
              .setCapacity(100)
              .setTitle("Old Title");

        request = new EventRequestDTO(
              "New Title",
              "Desc",
              null,
              "Loc",
              new BigDecimal("10.0"),
              200
        );
    }

    @Nested
    @DisplayName("Create Event Logic")
    class CreateEventTests {
        @Test
        @DisplayName("Should publish CreatedEvent and return DTO when save is successful")
        void shouldPublishCreatedEvent_WhenEventIsSavedSuccessfully() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn(ORGANIZER_ID);

            when(eventRepository.save(any(Event.class)))
                  .thenAnswer(i -> {
                      Event event = i.getArgument(0);
                      event.setId(EVENT_ID);
                      return event;
                  });

            EventResponseDTO result = eventService.createEvent(request);

            assertThat(result.id()).isEqualTo(EVENT_ID);
            verify(eventPublisher).publishEvent(any(EventCreatedEvent.class));
        }
    }

    @Nested
    @DisplayName("Update Event Logic")
    class UpdateEventTests {
        @Test
        @DisplayName("Should fail with ForbiddenException when a non-owner tries to update")
        void shouldThrowForbiddenException_WhenNonOwnerAttemptsUpdate() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn("malicious-user");

            when(eventRepository.findByIdAndStatusNot(EVENT_ID, CANCELLED))
                  .thenReturn(Optional.of(existingEvent));

            assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request))
                  .isInstanceOf(ForbiddenException.class);

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should sync with Redis when capacity is modified")
        void shouldPublishUpdatedEvent_WhenCapacityIsModified() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn(ORGANIZER_ID);

            when(eventRepository.findByIdAndStatusNot(EVENT_ID, CANCELLED))
                  .thenReturn(Optional.of(existingEvent));

            when(eventRepository.save(any()))
                  .thenReturn(existingEvent);

            eventService.updateEvent(EVENT_ID, request);

            verify(inventoryService).validateCapacityChange(eq(EVENT_ID), eq(200), any());
            verify(eventPublisher).publishEvent(any(EventUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should skip Redis sync when capacity remains unchanged")
        void shouldNotPublishUpdatedEvent_WhenCapacityRemainsSame() {
            EventRequestDTO sameCapacityRequest = new EventRequestDTO(
                  "Title",
                  "Desc",
                  null,
                  "Loc",
                  new BigDecimal("10.0"),
                  100
            );

            when(securityService.getAuthenticatedUserId())
                  .thenReturn(ORGANIZER_ID);

            when(eventRepository.findByIdAndStatusNot(EVENT_ID, CANCELLED))
                  .thenReturn(Optional.of(existingEvent));

            when(eventRepository.save(any()))
                  .thenReturn(existingEvent);

            eventService.updateEvent(EVENT_ID, sameCapacityRequest);

            verify(eventPublisher, never()).publishEvent(any(EventUpdatedEvent.class));
        }
    }

    @Nested
    @DisplayName("Event Cancellation and Deletion")
    class TerminationTests {
        @Test
        @DisplayName("Should set status to CANCELLED and notify listeners on cancellation")
        void shouldSetStatusToCancelled_WhenCancelIsRequestedByOwner() {
            when(securityService.getAuthenticatedUserId())
                  .thenReturn(ORGANIZER_ID);

            when(eventRepository.findByIdAndStatusNot(EVENT_ID, CANCELLED))
                  .thenReturn(Optional.of(existingEvent));

            eventService.cancelEvent(EVENT_ID);

            assertThat(existingEvent.getStatus()).isEqualTo(CANCELLED);
            verify(eventPublisher).publishEvent(any(EventCanceledEvent.class));
        }

        @Test
        @DisplayName("Should remove from DB only after successful Redis cleanup")
        void shouldDeleteFromDatabase_OnlyIfRedisDeletionSucceeds() {
            eventService.requestDeleteEvent(EVENT_ID);

            verify(eventPublisher).publishEvent(any(EventDeletedEvent.class));
            verify(eventRepository).deleteById(EVENT_ID);
        }

        @Test
        @DisplayName("Should abort DB deletion if Redis sync throws an exception")
        void shouldAbortDatabaseDeletion_WhenRedisSyncThrowsException() {
            doThrow(new RuntimeException("Redis error"))
                  .when(eventPublisher).publishEvent(any(EventDeletedEvent.class));

            assertThatThrownBy(() -> eventService.requestDeleteEvent(EVENT_ID))
                  .isInstanceOf(RuntimeException.class);

            verify(eventRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryTests {
        @Test
        @DisplayName("Should throw EventNotFoundException for missing or cancelled events")
        void shouldThrowNotFound_WhenAccessingCancelledEvent() {
            when(eventRepository.findByIdAndStatusNot(EVENT_ID, CANCELLED))
                  .thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEventId(EVENT_ID)).isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Should return price when event is active")
        void shouldReturnCorrectPrice_WhenEventIsActive() {
            BigDecimal price = new BigDecimal("49.99");

            when(eventRepository.findPriceByIdAndStatusNot(EVENT_ID, CANCELLED))
                  .thenReturn(Optional.of(price));

            assertThat(eventService.getPriceById(EVENT_ID)).isEqualByComparingTo(price);
        }
    }
}