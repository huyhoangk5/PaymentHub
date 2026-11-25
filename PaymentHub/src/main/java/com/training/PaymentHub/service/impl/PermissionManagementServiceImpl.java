package com.training.PaymentHub.service.impl;

import com.training.PaymentHub.model.dto.request.UpdateUserAccessRequest;
import com.training.PaymentHub.model.dto.response.PermissionCatalogResponse;
import com.training.PaymentHub.model.dto.response.UserPermissionSummaryResponse;
import com.training.PaymentHub.model.entity.Permission;
import com.training.PaymentHub.model.entity.Role;
import com.training.PaymentHub.model.entity.User;
import com.training.PaymentHub.model.entity.UserPermission;
import com.training.PaymentHub.repository.PermissionRepository;
import com.training.PaymentHub.repository.RoleRepository;
import com.training.PaymentHub.repository.UserPermissionRepository;
import com.training.PaymentHub.repository.UserRepository;
import com.training.PaymentHub.service.PermissionManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionManagementServiceImpl implements PermissionManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserPermissionSummaryResponse> searchUsers(String keyword, int page, int size) {
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<User> userPage = userRepository.searchUsers(normalizedKeyword, PageRequest.of(page, size));

        List<Long> userIds = userPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Long, List<UserPermission>> overridesByUserId = userIds.isEmpty()
                ? Collections.emptyMap()
                : userPermissionRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.groupingBy(up -> up.getUser().getId()));

        List<Permission> allPermissions = permissionRepository.findAllByOrderByModuleAscCodeAsc();
        Map<String, Permission> permByCode = allPermissions.stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity(), (a, b) -> a));

        return userPage.map(user -> toSummaryResponse(
                user,
                overridesByUserId.getOrDefault(user.getId(), Collections.emptyList()),
                permByCode
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionCatalogResponse getCatalog() {
        List<Role> roles = roleRepository.findAll();
        List<Permission> permissions = permissionRepository.findAllByOrderByModuleAscCodeAsc();

        List<PermissionCatalogResponse.RoleOption> roleOptions = roles.stream()
                .sorted(Comparator.comparing(Role::getId))
                .map(r -> PermissionCatalogResponse.RoleOption.builder()
                        .id(r.getId())
                        .roleCode(r.getRoleCode())
                        .roleName(r.getRoleName())
                        .description(r.getDescription())
                        .permissionCodes(r.getPermissions().stream()
                                .map(Permission::getCode)
                                .sorted()
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        List<PermissionCatalogResponse.PermissionOption> permOptions = permissions.stream()
                .map(p -> PermissionCatalogResponse.PermissionOption.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .module(p.getModule())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());

        return PermissionCatalogResponse.builder()
                .roles(roleOptions)
                .permissions(permOptions)
                .build();
    }

    @Override
    @Transactional
    public UserPermissionSummaryResponse updateUserAccess(Long userId, UpdateUserAccessRequest request) {
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với ID: " + userId));

        if (request.getRoleIds() != null) {
            Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            if (newRoles.size() != new HashSet<>(request.getRoleIds()).size()) {
                throw new IllegalArgumentException("Danh sách vai trò có ID không hợp lệ");
            }
            user.setRoles(newRoles);
            userRepository.save(user);
        }

        Set<Long> rolePermissionIds = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getId)
                .collect(Collectors.toSet());

        Set<Long> desiredPermissionIds = new LinkedHashSet<>(
                request.getPermissionIds() != null ? request.getPermissionIds() : Collections.emptyList()
        );

        Map<Long, Permission> allPermissionsMap = permissionRepository.findAllById(
                mergeIds(rolePermissionIds, desiredPermissionIds)
        ).stream().collect(Collectors.toMap(Permission::getId, Function.identity()));

        if (!allPermissionsMap.keySet().containsAll(desiredPermissionIds)) {
            throw new IllegalArgumentException("Danh sách quyền có ID không tồn tại trong hệ thống");
        }

        userPermissionRepository.deleteByUserId(userId);
        userPermissionRepository.flush();

        List<UserPermission> newOverrides = new ArrayList<>();

        // 1. Quyền được cấp thêm (GRANT): có trong desired nhưng không có trong Role
        for (Long permId : desiredPermissionIds) {
            if (!rolePermissionIds.contains(permId)) {
                newOverrides.add(UserPermission.builder()
                        .user(user)
                        .permission(allPermissionsMap.get(permId))
                        .accessType(UserPermission.ACCESS_GRANT)
                        .build());
            }
        }

        // 2. Quyền bị thu hồi bớt (REVOKE): có trong Role nhưng bị bỏ chọn
        for (Long rolePermId : rolePermissionIds) {
            if (!desiredPermissionIds.contains(rolePermId)) {
                newOverrides.add(UserPermission.builder()
                        .user(user)
                        .permission(allPermissionsMap.get(rolePermId))
                        .accessType(UserPermission.ACCESS_REVOKE)
                        .build());
            }
        }

        if (!newOverrides.isEmpty()) {
            userPermissionRepository.saveAll(newOverrides);
        }

        List<Permission> allCatalogPermissions = permissionRepository.findAllByOrderByModuleAscCodeAsc();
        Map<String, Permission> permByCode = allCatalogPermissions.stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity(), (a, b) -> a));

        return toSummaryResponse(user, newOverrides, permByCode);
    }

    private Set<Long> mergeIds(Set<Long> a, Set<Long> b) {
        Set<Long> merged = new HashSet<>(a);
        merged.addAll(b);
        return merged;
    }

    private UserPermissionSummaryResponse toSummaryResponse(
            User user,
            List<UserPermission> overrides,
            Map<String, Permission> permByCode
    ) {
        List<UserPermissionSummaryResponse.RoleItem> roleItems = user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getId))
                .map(r -> UserPermissionSummaryResponse.RoleItem.builder()
                        .id(r.getId())
                        .roleCode(r.getRoleCode())
                        .roleName(r.getRoleName())
                        .build())
                .collect(Collectors.toList());

        Set<String> rolePerms = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> grantedPerms = overrides.stream()
                .filter(up -> UserPermission.ACCESS_GRANT.equalsIgnoreCase(up.getAccessType()))
                .map(up -> up.getPermission().getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> revokedPerms = overrides.stream()
                .filter(up -> UserPermission.ACCESS_REVOKE.equalsIgnoreCase(up.getAccessType()))
                .map(up -> up.getPermission().getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> effectiveCodes = new LinkedHashSet<>(rolePerms);
        effectiveCodes.addAll(grantedPerms);
        effectiveCodes.removeAll(revokedPerms);

        List<UserPermissionSummaryResponse.PermissionItem> effectiveItems = effectiveCodes.stream()
                .map(permByCode::get)
                .filter(Objects::nonNull)
                .map(p -> UserPermissionSummaryResponse.PermissionItem.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .module(p.getModule())
                        .build())
                .collect(Collectors.toList());

        return UserPermissionSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .enabled(user.getEnabled())
                .roles(roleItems)
                .rolePermissions(new ArrayList<>(rolePerms))
                .grantedPermissions(new ArrayList<>(grantedPerms))
                .revokedPermissions(new ArrayList<>(revokedPerms))
                .effectivePermissions(effectiveItems)
                .build();
    }
}
