package com.ga.common.events;

public record NotificationEvent(Long userId, String action, String entityType,
                                Long entityId, String details) {
}
