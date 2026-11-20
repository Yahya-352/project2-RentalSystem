package com.ga.audit_service.dto.response;

import com.ga.audit_service.model.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String entityType,
        Long entityId,
        String details,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt());
    }
}
