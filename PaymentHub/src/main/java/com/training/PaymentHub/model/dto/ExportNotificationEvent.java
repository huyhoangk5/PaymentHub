package com.training.PaymentHub.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportNotificationEvent {
    private String jobId;
    private String userId;
    private String status;
    private Integer progress;
    private String fileUrl;
    private String downloadUrl;
    private Long totalRecords;
    private String errorMessage;
    private LocalDateTime updatedAt;
}

