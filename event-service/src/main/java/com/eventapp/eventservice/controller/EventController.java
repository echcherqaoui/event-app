package com.eventapp.eventservice.controller;

import com.eventapp.eventservice.dto.request.EventRequestDTO;
import com.eventapp.eventservice.dto.response.EventResponseDTO;
import com.eventapp.eventservice.service.IEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.base-path}/events")
@RequiredArgsConstructor
public class EventController {

    private final IEventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(@Valid @RequestBody EventRequestDTO eventRequest) {
        EventResponseDTO createdEvent = eventService.createEvent(eventRequest);

        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<EventResponseDTO>> getMyEvents(
          @PageableDefault(sort = "eventDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.getMyEvents(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable("id") Long id,
                                                        @Valid @RequestBody EventRequestDTO eventRequest) {
        EventResponseDTO updatedEvent = eventService.updateEvent(id, eventRequest);
        return ResponseEntity.ok(updatedEvent);
    }

    @GetMapping
    public ResponseEntity<Page<EventResponseDTO>> getAllEvents(
          @PageableDefault(sort = "eventDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
              eventService.getAllEvents(pageable)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<Page<EventResponseDTO>> getActiveEvents(
          @PageableDefault(sort = "eventDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
              eventService.getActiveEvents(pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getEventId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(eventService.getEventId(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelEvent(@PathVariable("id") Long id) {
        eventService.cancelEvent(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") Long id) {
        eventService.requestDeleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}