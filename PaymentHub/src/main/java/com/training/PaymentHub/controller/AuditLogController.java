package com.training.PaymentHub.controller;

import com.training.PaymentHub.model.dto.response.ApiResponse;
import com.training.PaymentHub.model.dto.response.AuditLogResponse;
import com.training.PaymentHub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(value = "module", required = false, defaultValue = "COMPONENT") String module,
            @RequestParam(value = "recordId", required = false) String recordId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Page<AuditLogResponse> result = auditLogService.getAuditLogs(module, recordId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}