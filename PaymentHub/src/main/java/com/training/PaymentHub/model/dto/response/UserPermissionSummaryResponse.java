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
public class UserPermissionSummaryResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String enabled;
    private List<RoleItem> roles;
    private List<String> rolePermissions;
    private List<String> grantedPermissions;
    private List<String> revokedPermissions;
    private List<PermissionItem> effectivePermissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleItem {
        private Long id;
        private String roleCode;
        private String roleName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItem {
        private Long id;
        private String code;
        private String name;
        private String module;
    }
}
