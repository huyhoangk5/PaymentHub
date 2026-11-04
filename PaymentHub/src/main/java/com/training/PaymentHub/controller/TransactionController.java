package com.training.PaymentHub.controller;

import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import com.training.PaymentHub.model.dto.response.ApiResponse;
import com.training.PaymentHub.model.dto.response.ExportJobResponse;
import com.training.PaymentHub.model.dto.response.TransactionLogResponse;
import com.training.PaymentHub.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<TransactionLogResponse>>> searchTransactions(
            @RequestBody(required = false) TransactionSearchRequest req) {
        Page<TransactionLogResponse> data = transactionService.searchTransactions(req);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<ExportJobResponse>> requestExportExcel(
            @RequestBody(required = false) TransactionSearchRequest req,
            Principal principal) {
        String userId = extractUserId(principal);
        ExportJobResponse job = transactionService.requestExportExcel(req, userId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping("/export/my-jobs")
    public ResponseEntity<ApiResponse<List<ExportJobResponse>>> getMyExportJobs(Principal principal) {
        String userId = extractUserId(principal);
        List<ExportJobResponse> jobs = transactionService.getMyExportJobs(userId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/export/{jobId}")
    public ResponseEntity<ApiResponse<ExportJobResponse>> getExportJobStatus(
            @PathVariable("jobId") String jobId,
            Principal principal) {
        String userId = extractUserId(principal);
        ExportJobResponse job = transactionService.getExportJobById(jobId, userId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping("/export/{jobId}/download")
    public ResponseEntity<InputStreamResource> downloadExportFile(
            @PathVariable("jobId") String jobId,
            Principal principal) {
        String userId = extractUserId(principal);
        InputStream stream = transactionService.downloadExportFile(jobId, userId);
        String fileName = "Lich_su_giao_dich_" + jobId.substring(0, Math.min(8, jobId.length())) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(new InputStreamResource(stream));
    }

    private String extractUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("Không xác định được thông tin người dùng đăng nhập");
        }
        return principal.getName();
    }
}
