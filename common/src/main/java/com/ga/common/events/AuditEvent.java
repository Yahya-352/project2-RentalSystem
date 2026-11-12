package com.ga.common.events;

public record AuditEvent(Long userId, String action, String entityType,
                         Long entityId, String details) {
}
