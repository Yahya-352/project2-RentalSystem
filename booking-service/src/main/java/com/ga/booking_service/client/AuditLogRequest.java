package com.ga.booking_service.client;

public record AuditLogRequest(Long userId, String action, String entityType,
                              Long entityId, String details) {
}
