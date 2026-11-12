package com.ga.common.events;

public record AuditEvent(Long userId, String eventName, Object data) {
}
