package com.training.PaymentHub.model.dto.response;

import com.training.PaymentHub.constant.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCategoryResponse {
    private Long id;
    private String paramName;
    private String paramValue;
    private String paramType;
    private String description;
    private String componentCode;
    private CommonStatus status;
    private Integer isActive;
    private Integer isDisplay;
    private String newData;
    private LocalDate effectiveDate;
    private LocalDate endEffectiveDate;
}