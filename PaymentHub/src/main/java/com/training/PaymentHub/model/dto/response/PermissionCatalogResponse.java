package com.training.PaymentHub.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCatalogResponse {

    private List<RoleOption> roles;
    private List<PermissionOption> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleOption {
        private Long id;
        private String roleCode;
        private String roleName;
        private String description;
        private List<String> permissionCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionOption {
        private Long id;
        private String code;
        private String name;
        private String module;
        private String description;
    }
}
