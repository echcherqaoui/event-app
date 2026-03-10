package com.eventapp.eventservice.events;

import com.eventapp.eventservice.enums.EventStatus;

public record EventDeletedEvent(Long eventId, EventStatus status) {}