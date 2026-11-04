package com.training.PaymentHub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ComponentUpdateRequest {
    @NotNull(message = "ID cấu phần không được để trống")
    private Long id;

    @NotBlank(message = "Mã cấu phần không được để trống")
    @Size(max = 20, message = "Mã cấu phần tối đa 20 ký tự")
    private String componentCode;

    @NotBlank(message = "Tên cấu phần không được để trống")
    @Size(max = 150, message = "Tên cấu phần tối đa 150 ký tự")
    private String componentName;

    @Size(max = 1500, message = "Chuẩn tin điện tối đa 1500 ký tự")
    private String messageType;

    @Size(max = 100, message = "Tên kết nối tối đa 100 ký tự")
    private String connectionMethod;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDate effectiveDate;

    private LocalDate endEffectiveDate;

    private Boolean checkToken;

    private Integer status;
}
