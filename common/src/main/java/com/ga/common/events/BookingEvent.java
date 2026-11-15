package com.ga.common.events;

import java.time.LocalDateTime;

public record BookingEvent(
        String eventId,
        String eventType,
        Long bookingId,
        Long actorId,
        Long renterId,
        Long ownerId,
        String message,
        Object booking,
        LocalDateTime occurredAt
) {}
