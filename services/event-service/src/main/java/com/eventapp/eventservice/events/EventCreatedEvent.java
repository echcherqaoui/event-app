package com.eventapp.eventservice.events;

import com.eventapp.eventservice.enums.EventStatus;

public record EventCreatedEvent(Long eventId,
                                int capacity,
                                EventStatus status) {}
