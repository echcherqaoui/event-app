package com.eventapp.eventservice.events;

import com.eventapp.eventservice.enums.EventStatus;

public record EventUpdatedEvent(Long eventId,
                                int newCapacity,
                                EventStatus status) {}