package com.training.PaymentHub.security;

import com.training.PaymentHub.model.entity.Permission;
import com.training.PaymentHub.model.entity.Role;
import com.training.PaymentHub.model.entity.User;
import com.training.PaymentHub.model.entity.UserPermission;
import com.training.PaymentHub.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tính toán danh sách quyền hiệu lực (Effective Permissions) của User
 * dựa trên Roles mặc định kết hợp với bảng PMH_USER_PERMISSIONS (GRANT / REVOKE).
 */
@Component
@RequiredArgsConstructor
public class UserPermissionResolver {

    private final UserPermissionRepository userPermissionRepository;

    public List<String> resolveAuthorities(User user) {
        List<UserPermission> overrides = userPermissionRepository.findByUserId(user.getId());

        Set<String> rolePermCodes = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> grantedCodes = overrides.stream()
                .filter(up -> UserPermission.ACCESS_GRANT.equalsIgnoreCase(up.getAccessType()))
                .map(up -> up.getPermission().getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> revokedCodes = overrides.stream()
                .filter(up -> UserPermission.ACCESS_REVOKE.equalsIgnoreCase(up.getAccessType()))
                .map(up -> up.getPermission().getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> effectivePerms = new LinkedHashSet<>(rolePermCodes);
        effectivePerms.addAll(grantedCodes);
        effectivePerms.removeAll(revokedCodes);

        // Giữ tương thích ngược: tự động suy diễn GROUP_CATEGORY_* từ COMPONENT_* nếu không bị REVOKE tường minh
        List<String> snapshot = new ArrayList<>(effectivePerms);
        for (String perm : snapshot) {
            if (perm != null && perm.startsWith("COMPONENT_")) {
                String groupCatPerm = perm.replace("COMPONENT_", "GROUP_CATEGORY_");
                if (!revokedCodes.contains(groupCatPerm)) {
                    effectivePerms.add(groupCatPerm);
                }
            }
        }

        // Bổ sung Role codes vào danh sách authorities
        for (Role role : user.getRoles()) {
            if (role.getRoleCode() != null) {
                effectivePerms.add(role.getRoleCode());
            }
        }

        return new ArrayList<>(effectivePerms);
    }
}
