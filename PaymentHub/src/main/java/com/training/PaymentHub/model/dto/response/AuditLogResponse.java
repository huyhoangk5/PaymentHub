package com.training.PaymentHub.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String performedBy;
    
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime actionDate;
    
    private String action;
    private String ipAddress;
    private String description;
    private Integer statusBefore;
    private Integer statusAfter;
}