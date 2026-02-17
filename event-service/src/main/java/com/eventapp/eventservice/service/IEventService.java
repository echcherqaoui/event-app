package com.eventapp.eventservice.service;

import com.eventapp.eventservice.dto.request.EventRequestDTO;
import com.eventapp.eventservice.dto.response.EventResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEventService {
    EventResponseDTO createEvent(EventRequestDTO eventRequest);

    Page<EventResponseDTO> getAllEvents(Pageable pageable);

    EventResponseDTO getEventId(Long id);

    EventResponseDTO updateEvent(Long id, EventRequestDTO eventRequest);

    Page<EventResponseDTO> getMyEvents(Pageable pageable);

    Page<EventResponseDTO> getActiveEvents(Pageable pageable);

    void requestDeleteEvent(Long eventId);



    void cancelEvent(Long eventId);
}
