package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;

public interface AuditLogService {
    Page<AuditLogResponse> getAuditLogs(String module, String recordId, int page, int size);
    
    void saveLog(String module, String recordId, String action, String performedBy, 
                 String description, Integer statusBefore, Integer statusAfter, 
                 String ipAddress, String oldData, String newDataLog);
}