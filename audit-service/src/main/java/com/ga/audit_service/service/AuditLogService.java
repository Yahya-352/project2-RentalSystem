package com.ga.audit_service.service;


import com.ga.audit_service.dto.response.AuditLogResponse;
import com.ga.audit_service.model.AuditLog;
import com.ga.audit_service.repository.AuditLogRepository;
import com.ga.common.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public void log(Long userId, String action, String entityType,
                    Long entityId, String details , LocalDateTime timestamp , String eventId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        auditLog.setCreatedAt(timestamp != null ? timestamp : LocalDateTime.now());
        auditLog.setEventId(eventId);
        auditLogRepository.save(auditLog);
    }

    /**
     * Lists audit entries for the admin. All filters are optional and combined.
     *
     * @param action     exact action name, e.g. BOOKING_APPROVED
     * @param entityType exact entity type, e.g. Booking
     * @param userId     the user who did the action
     * @param pageable   page number, size and sort
     * @return one page of audit entries
     */
    public PageResponse<AuditLogResponse> getLogs(String action, String entityType, Long userId,
                                                  Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action.trim().toUpperCase()));
        }
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType.trim()));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }

        return PageResponse.from(auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from));
    }
}
