package com.training.PaymentHub.model.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobResponse {
    private Long id;
    private String jobId;
    private String userId;
    private String status;
    private Integer progress;
    private String fileUrl;
    private String downloadUrl;
    private Long totalRecords;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean expired;
}

