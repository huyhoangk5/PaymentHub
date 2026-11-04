package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import com.training.PaymentHub.model.dto.response.ExportJobResponse;
import com.training.PaymentHub.model.dto.response.TransactionLogResponse;
import org.springframework.data.domain.Page;

import java.io.InputStream;
import java.util.List;

public interface TransactionService {
    Page<TransactionLogResponse> searchTransactions(TransactionSearchRequest req);
    ExportJobResponse requestExportExcel(TransactionSearchRequest req, String userId);
    List<ExportJobResponse> getMyExportJobs(String userId);
    ExportJobResponse getExportJobById(String jobId, String userId);
    InputStream downloadExportFile(String jobId, String userId);
}

