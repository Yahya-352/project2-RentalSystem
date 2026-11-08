package com.ga.audit_service.controller;

import com.ga.audit_service.dto.request.AuditLogRequest;
import com.ga.audit_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping
    public ResponseEntity<Void> createAuditLog(@RequestHeader("X-Internal-Key") String key,
                                               @RequestBody AuditLogRequest request) {

        if (!internalApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        auditLogService.log(request.userId(), request.action(), request.entityType(),
                request.entityId(), request.details());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
