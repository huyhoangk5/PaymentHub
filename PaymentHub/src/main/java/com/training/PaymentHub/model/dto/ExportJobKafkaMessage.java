package com.training.PaymentHub.model.dto;

import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobKafkaMessage {
    private String jobId;
    private String userId;
    private TransactionSearchRequest filter;
}

