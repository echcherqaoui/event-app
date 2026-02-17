package com.eventapp.eventservice.repository;

import com.eventapp.eventservice.enums.EventStatus;
import com.eventapp.eventservice.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAllByStatusNot(EventStatus status, Pageable pageable);



    Page<Event> findByOrganizerId(String organizerId, Pageable pageable);

    Optional<Event> findByIdAndStatusNot(Long id, EventStatus status);


    @Query("""
                SELECT e FROM Event e
                    WHERE e.capacity > 0
                        AND e.status = status
                        AND e.eventDate > CURRENT_TIMESTAMP
           """)
    Page<Event> findActiveEvents(EventStatus status, Pageable pageable);

}