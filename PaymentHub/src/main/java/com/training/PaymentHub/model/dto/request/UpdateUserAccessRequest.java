package com.training.PaymentHub.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserAccessRequest {
    private List<Long> roleIds;

    @NotNull(message = "Danh sách permissionIds không được để trống")
    private List<Long> permissionIds;
}
