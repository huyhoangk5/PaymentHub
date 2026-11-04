package com.training.PaymentHub.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GroupCategoryUpdateRequest {

    @NotNull(message = "ID không được để trống")
    private Long id;

    @NotBlank(message = "Tên thành phần không được để trống")
    @Size(max = 255, message = "Tên thành phần tối đa 255 ký tự")
    private String paramName;

    @NotBlank(message = "Giá trị thành phần không được để trống")
    @Size(max = 255, message = "Giá trị thành phần tối đa 255 ký tự")
    private String paramValue;

    @NotBlank(message = "Danh mục theo nhóm không được để trống")
    @Size(max = 255, message = "Danh mục theo nhóm tối đa 255 ký tự")
    private String paramType;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @NotBlank(message = "Mã cấu phần xử lý không được để trống")
    private String componentCode;

    private Integer isActive;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDate effectiveDate;

    private LocalDate endEffectiveDate;
}