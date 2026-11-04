package com.training.PaymentHub.service;

import java.io.InputStream;
import java.nio.file.Path;

public interface MinioStorageService {
    String uploadExportFile(String jobId, Path localFilePath);
    InputStream downloadExportFile(String jobId);
    void deleteExportFile(String jobId);
    boolean fileExists(String jobId);
}

