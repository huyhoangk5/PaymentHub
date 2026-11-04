package com.training.PaymentHub.service.impl;

import com.training.PaymentHub.service.MinioStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements MinioStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.minio.bucket-name:export-files}")
    private String bucketName;

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public String uploadExportFile(String jobId, Path localFilePath) {
        String objectName = buildObjectName(jobId);
        try {
            ensureBucketExists();
            long fileSize = Files.size(localFilePath);

            try (InputStream is = Files.newInputStream(localFilePath)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(is, fileSize, -1)
                                .contentType(EXCEL_CONTENT_TYPE)
                                .build()
                );
            }

            String fileUrl = String.format("%s/%s/%s", endpoint, bucketName, objectName);
            log.info("Upload MinIO thành công cho Job {}: {} ({} bytes)", jobId, fileUrl, fileSize);
            return fileUrl;
        } catch (Exception e) {
            log.error("Lỗi upload file Excel lên MinIO cho Job {}: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Không thể tải file Excel lên MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadExportFile(String jobId) {
        String objectName = buildObjectName(jobId);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi tải file từ MinIO cho Job {}: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Không tìm thấy hoặc không thể đọc file Excel trên MinIO", e);
        }
    }

    @Override
    public void deleteExportFile(String jobId) {
        String objectName = buildObjectName(jobId);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Đã xóa file trên MinIO cho Job {}: {}", jobId, objectName);
        } catch (Exception e) {
            log.warn("Không thể xóa file trên MinIO cho Job {}: {}", jobId, e.getMessage());
        }
    }

    @Override
    public boolean fileExists(String jobId) {
        String objectName = buildObjectName(jobId);
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildObjectName(String jobId) {
        return "exports/" + jobId + ".xlsx";
    }

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }
}

