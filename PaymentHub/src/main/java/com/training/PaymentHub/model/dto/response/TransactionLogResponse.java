package com.training.PaymentHub.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLogResponse {
    private Long id;
    private String transactionCode;
    private String accountNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private String description;
    private String referenceNo;
    private LocalDateTime updatedAt;
}

