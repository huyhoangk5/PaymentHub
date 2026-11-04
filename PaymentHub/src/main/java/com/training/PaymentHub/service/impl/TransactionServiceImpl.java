package com.training.PaymentHub.service.impl;

import com.training.PaymentHub.constant.ExportJobStatus;
import com.training.PaymentHub.kafka.ExportJobProducer;
import com.training.PaymentHub.model.dto.ExportJobKafkaMessage;
import com.training.PaymentHub.model.dto.ExportNotificationEvent;
import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import com.training.PaymentHub.model.dto.response.ExportJobResponse;
import com.training.PaymentHub.model.dto.response.TransactionLogResponse;
import com.training.PaymentHub.model.entity.ExportJob;
import com.training.PaymentHub.model.entity.TransactionLog;
import com.training.PaymentHub.repository.ExportJobRepository;
import com.training.PaymentHub.repository.TransactionLogRepository;
import com.training.PaymentHub.service.MinioStorageService;
import com.training.PaymentHub.service.RedisProgressService;
import com.training.PaymentHub.service.TransactionService;
import com.training.PaymentHub.specification.TransactionLogSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionLogRepository transactionLogRepository;
    private final ExportJobRepository exportJobRepository;
    private final ExportJobProducer exportJobProducer;
    private final RedisProgressService redisProgressService;
    private final MinioStorageService minioStorageService;

    @Value("${app.export.expiration-hours:24}")
    private long expirationHours;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionLogResponse> searchTransactions(TransactionSearchRequest req) {
        if (req == null) {
            req = new TransactionSearchRequest();
        }
        validateFilter(req);

        int page = req.getPage() != null ? Math.max(req.getPage(), 0) : 0;
        int size = (req.getSize() != null && req.getSize() > 0) ? Math.min(req.getSize(), 200) : 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<TransactionLog> entities = transactionLogRepository.findAll(TransactionLogSpec.filter(req), pageable);
        return entities.map(this::toTransactionResponse);
    }

    @Override
    @Transactional
    public ExportJobResponse requestExportExcel(TransactionSearchRequest req, String userId) {
        if (req == null) {
            req = new TransactionSearchRequest();
        }
        validateFilter(req);

        String jobId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // 1. Lưu ExportJob vào Database trước khi gửi Kafka (Single Source of Truth)
        ExportJob job = ExportJob.builder()
                .jobId(jobId)
                .userId(userId)
                .status(ExportJobStatus.PENDING)
                .progress(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        job = exportJobRepository.saveAndFlush(job);

        // 2. Lưu bộ lọc & trạng thái khởi tạo vào Redis (TTL 24h)
        redisProgressService.saveJobFilter(jobId, req);
        redisProgressService.saveJobState(ExportNotificationEvent.builder()
                .jobId(jobId)
                .userId(userId)
                .status(ExportJobStatus.PENDING)
                .progress(0)
                .updatedAt(now)
                .build());

        // 3. Đẩy message vào Kafka để Worker xử lý bất đồng bộ
        ExportJobKafkaMessage kafkaMessage = ExportJobKafkaMessage.builder()
                .jobId(jobId)
                .userId(userId)
                .filter(req)
                .build();
        exportJobProducer.sendExportJob(kafkaMessage);

        return toExportJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportJobResponse> getMyExportJobs(String userId) {
        return exportJobRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(20)
                .map(this::toExportJobResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getExportJobById(String jobId, String userId) {
        ExportJob job = findAndVerifyOwnership(jobId, userId);
        return toExportJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadExportFile(String jobId, String userId) {
        ExportJob job = findAndVerifyOwnership(jobId, userId);

        if (!ExportJobStatus.SUCCESS.equalsIgnoreCase(job.getStatus())) {
            throw new IllegalArgumentException("File xuất chưa hoàn tất hoặc quá trình xuất đã gặp lỗi");
        }

        if (isExpired(job)) {
            minioStorageService.deleteExportFile(jobId);
            throw new IllegalArgumentException("File xuất đã quá thời hạn lưu trữ 24 giờ. Vui lòng thực hiện xuất lại.");
        }

        if (!minioStorageService.fileExists(jobId)) {
            throw new IllegalArgumentException("File xuất không tồn tại trên hệ thống lưu trữ hoặc đã bị dọn dẹp. Vui lòng bấm Xuất Excel để tạo file mới.");
        }

        return minioStorageService.downloadExportFile(jobId);
    }

    private ExportJob findAndVerifyOwnership(String jobId, String userId) {
        ExportJob job = exportJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu xuất file: " + jobId));

        if (job.getUserId() != null && !job.getUserId().equalsIgnoreCase(userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập hoặc tải file của người dùng khác");
        }
        return job;
    }

    private void validateFilter(TransactionSearchRequest req) {
        if (req.getFromDate() != null && req.getToDate() != null && req.getToDate().isBefore(req.getFromDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu");
        }
        if (req.getMinAmount() != null && req.getMaxAmount() != null && req.getMaxAmount().compareTo(req.getMinAmount()) < 0) {
            throw new IllegalArgumentException("Số tiền tối đa không được nhỏ hơn số tiền tối thiểu");
        }
    }

    private boolean isExpired(ExportJob job) {
        LocalDateTime refTime = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt();
        if (refTime == null) return false;
        return Duration.between(refTime, LocalDateTime.now()).toHours() >= expirationHours;
    }

    private TransactionLogResponse toTransactionResponse(TransactionLog entity) {
        return TransactionLogResponse.builder()
                .id(entity.getId())
                .transactionCode(entity.getTransactionCode())
                .accountNo(entity.getAccountNo())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .description(entity.getDescription())
                .referenceNo(entity.getReferenceNo())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ExportJobResponse toExportJobResponse(ExportJob job) {
        boolean expired = isExpired(job);
        boolean isSuccess = ExportJobStatus.SUCCESS.equalsIgnoreCase(job.getStatus());
        boolean fileAvailable = isSuccess && !expired && minioStorageService.fileExists(job.getJobId());
        boolean effectivelyExpired = expired || (isSuccess && !fileAvailable);
        String downloadUrl = fileAvailable ? "/api/transactions/export/" + job.getJobId() + "/download" : null;

        return ExportJobResponse.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .userId(job.getUserId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .fileUrl(job.getFileUrl())
                .downloadUrl(downloadUrl)
                .totalRecords(job.getTotalRecords())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .expired(effectivelyExpired)
                .build();
    }
}

