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
public class ComponentResponse {
    private Long id;
    private String componentCode;
    private String componentName;
    private String messageType;
    private String connectionMethod;
    private Boolean checkToken;
    private CommonStatus status;
    private LocalDate effectiveDate;
    private LocalDate endEffectiveDate;
    private int isActive;
    private int isDispllay;
    private String newData;

    public int getIsDisplay() {
        return isDispllay;
    }

    public void setIsDisplay(int isDisplay) {
        this.isDispllay = isDisplay;
    }
}
