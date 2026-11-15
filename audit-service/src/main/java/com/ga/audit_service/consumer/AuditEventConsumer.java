package com.ga.audit_service.consumer;

import com.ga.audit_service.repository.AuditLogRepository;
import com.ga.audit_service.service.AuditLogService;
import com.ga.common.events.AuditEvent;
import com.ga.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.internals.Topic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = Topics.AUDIT)
    public void consume(AuditEvent auditEvent) {

        if (auditEvent.eventId() != null && auditLogRepository.existsByEventId(auditEvent.eventId())) {
            return;
        }

        auditLogService.log(auditEvent.userId() , auditEvent.action(),
                auditEvent.entityType(), auditEvent.entityId(),
                  auditEvent.details() , auditEvent.occurredAt() , auditEvent.eventId());
    }

}
