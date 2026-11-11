package com.ga.payment_service.client;

public record AuditLogRequest(Long userId, String action, String entityType,
                              Long entityId, String details) {
}
