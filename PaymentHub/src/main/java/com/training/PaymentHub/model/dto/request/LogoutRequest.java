package com.training.PaymentHub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "Refresh Token không được để trống")
    private String refreshToken;
}