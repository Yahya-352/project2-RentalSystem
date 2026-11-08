package com.ga.audit_service.dto.request;

public record AuditLogRequest(Long userId, String action,
                              String entityType, Long entityId, String details) {}
