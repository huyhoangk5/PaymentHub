package com.training.PaymentHub.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSearchRequest {

    private String keyword;
    private String transactionCode;
    private String accountNo;
    private String status;
    private String referenceNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;
}

