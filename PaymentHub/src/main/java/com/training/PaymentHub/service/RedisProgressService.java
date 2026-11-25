package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.ExportNotificationEvent;
import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;

public interface RedisProgressService {
    void saveJobState(ExportNotificationEvent state);
    ExportNotificationEvent getJobState(String jobId);
    void saveJobFilter(String jobId, TransactionSearchRequest filter);
    TransactionSearchRequest getJobFilter(String jobId);
    TransactionSearchRequest getJobFilterIfPresent(String jobId);
    void publishNotification(ExportNotificationEvent event);
}

