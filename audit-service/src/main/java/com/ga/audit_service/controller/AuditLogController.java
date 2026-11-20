package com.ga.audit_service.controller;

import com.ga.audit_service.dto.response.AuditLogResponse;
import com.ga.audit_service.service.AuditLogService;
import com.ga.common.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<AuditLogResponse> getLogs(@RequestParam(required = false) String action,
                                                  @RequestParam(required = false) String entityType,
                                                  @RequestParam(required = false) Long userId,
                                                  @PageableDefault(size = 20, sort = "createdAt",
                                                          direction = Sort.Direction.DESC) Pageable pageable) {
        return auditLogService.getLogs(action, entityType, userId, pageable);
    }
}
