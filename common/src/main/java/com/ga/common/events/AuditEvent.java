package com.ga.common.events;

import java.time.LocalDateTime;

public record AuditEvent(Long userId, String action, String entityType,
                         Long entityId, String details , LocalDateTime occurredAt , String eventId) {
}
