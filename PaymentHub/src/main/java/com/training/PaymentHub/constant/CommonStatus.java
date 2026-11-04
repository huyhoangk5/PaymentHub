package com.training.PaymentHub.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CommonStatus {
    NEW(1, "Mới"),
    PENDING(3, "Chờ duyệt"),
    APPROVED(4, "Đã duyệt"),
    REJECTED(5, "Từ chối"),
    CANCELLED(7, "Hủy duyệt");

    private final Integer code;
    private final String description;

    CommonStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    // Giúp Jackson serialize/deserialize đúng giá trị số (1, 3, 4, 5, 7) khi nhận/trả JSON
    @JsonValue
    public Integer getCode() {
        return code;
    }

    // Mapping từ số nguyên lên Enum khi nhận JSON từ Client hoặc ánh xạ từ Database
    @JsonCreator
    public static CommonStatus fromCode(Integer code) {
        if (code == null) return null;
        for (CommonStatus status : CommonStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
