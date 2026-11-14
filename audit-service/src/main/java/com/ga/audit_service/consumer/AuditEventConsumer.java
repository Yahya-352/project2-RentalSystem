package com.ga.audit_service.consumer;

import com.ga.audit_service.service.AuditLogService;
import com.ga.common.events.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    @KafkaListener
    public void consume(AuditEvent auditEvent) {
        auditLogService.log(auditEvent.userId() , auditEvent.action(),
                auditEvent.entityType(), auditEvent.entityId(),
                auditEvent.details());
    }

}
