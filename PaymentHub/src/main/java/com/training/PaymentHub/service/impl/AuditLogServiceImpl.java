package com.training.PaymentHub.service.impl;

import com.training.PaymentHub.model.dto.response.AuditLogResponse;
import com.training.PaymentHub.model.entity.AuditLog;
import com.training.PaymentHub.repository.AuditLogRepository;
import com.training.PaymentHub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String module, String recordId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logPage = auditLogRepository.findByModuleAndRecordIdOrderByActionDateDesc(module, recordId, pageable);
        return logPage.map(log -> AuditLogResponse.builder()
                .id(log.getId())
                .performedBy(log.getPerformedBy())
                .actionDate(log.getActionDate())
                .action(log.getAction())
                .ipAddress(log.getIpAddress())
                .description(log.getDescription())
                .statusBefore(log.getStatusBefore())
                .statusAfter(log.getStatusAfter())
                .build());
    }

    @Override
    @Transactional
    public void saveLog(String module, String recordId, String action, String performedBy, 
                        String description, Integer statusBefore, Integer statusAfter, 
                        String ipAddress, String oldData, String newDataLog) {
        AuditLog log = AuditLog.builder()
                .module(module)
                .recordId(recordId)
                .action(action)
                .performedBy(performedBy)
                .actionDate(LocalDateTime.now())
                .description(description)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .ipAddress(ipAddress)
                .oldData(oldData)
                .newDataLog(newDataLog)
                .build();
        auditLogRepository.save(log);
    }
}