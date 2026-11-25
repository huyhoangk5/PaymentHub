package com.training.PaymentHub;

import com.training.PaymentHub.model.dto.request.UpdateUserAccessRequest;
import com.training.PaymentHub.model.dto.response.UserPermissionSummaryResponse;
import com.training.PaymentHub.model.entity.Permission;
import com.training.PaymentHub.model.entity.Role;
import com.training.PaymentHub.model.entity.User;
import com.training.PaymentHub.model.entity.UserPermission;
import com.training.PaymentHub.repository.PermissionRepository;
import com.training.PaymentHub.repository.RoleRepository;
import com.training.PaymentHub.repository.UserPermissionRepository;
import com.training.PaymentHub.repository.UserRepository;
import com.training.PaymentHub.security.UserPermissionResolver;
import com.training.PaymentHub.service.impl.PermissionManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionManagementTest {

    @Mock
    private UserPermissionRepository userPermissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;

    private UserPermissionResolver resolver;

    @InjectMocks
    private PermissionManagementServiceImpl managementService;

    private Permission permComponentView;
    private Permission permComponentCreate;
    private Permission permGroupCatView;
    private Permission permAuditView;
    private Role viewerRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        resolver = new UserPermissionResolver(userPermissionRepository);

        permComponentView = Permission.builder().id(9L).code("COMPONENT_VIEW").name("Xem cấu phần").module("COMPONENT").build();
        permComponentCreate = Permission.builder().id(10L).code("COMPONENT_CREATE").name("Tạo cấu phần").module("COMPONENT").build();
        permGroupCatView = Permission.builder().id(42L).code("GROUP_CATEGORY_VIEW").name("Xem danh mục nhóm").module("GROUP_CATEGORY").build();
        permAuditView = Permission.builder().id(17L).code("AUDIT_VIEW").name("Xem audit log").module("AUDIT").build();

        viewerRole = Role.builder()
                .id(22L)
                .roleCode("ROLE_VIEWER")
                .roleName("Người chỉ xem")
                .permissions(Set.of(permComponentView))
                .build();

        testUser = User.builder()
                .id(100L)
                .username("tester01")
                .fullName("Tester 01")
                .enabled("Y")
                .roles(Set.of(viewerRole))
                .build();
    }

    @Test
    void resolveAuthorities_shouldIncludeGrantedAndExcludeRevoked() {
        UserPermission grantAudit = UserPermission.builder()
                .user(testUser)
                .permission(permAuditView)
                .accessType(UserPermission.ACCESS_GRANT)
                .build();
        UserPermission revokeGroupCat = UserPermission.builder()
                .user(testUser)
                .permission(permGroupCatView)
                .accessType(UserPermission.ACCESS_REVOKE)
                .build();

        when(userPermissionRepository.findByUserId(100L)).thenReturn(List.of(grantAudit, revokeGroupCat));

        List<String> authorities = resolver.resolveAuthorities(testUser);

        assertThat(authorities).contains("COMPONENT_VIEW", "AUDIT_VIEW", "ROLE_VIEWER");
        assertThat(authorities).doesNotContain("GROUP_CATEGORY_VIEW", "COMPONENT_CREATE");
    }

    @Test
    void updateUserAccess_shouldSaveGrantAndRevokeCorrectly() {
        when(userRepository.findByIdWithRolesAndPermissions(100L)).thenReturn(Optional.of(testUser));
        when(permissionRepository.findAllById(any())).thenReturn(List.of(permComponentView, permComponentCreate));
        when(permissionRepository.findAllByOrderByModuleAscCodeAsc())
                .thenReturn(List.of(permComponentView, permComponentCreate, permAuditView));

        // Bỏ COMPONENT_VIEW (vốn có từ Role -> REVOKE) và thêm COMPONENT_CREATE (chưa có -> GRANT)
        UpdateUserAccessRequest req = new UpdateUserAccessRequest();
        req.setPermissionIds(List.of(10L));

        UserPermissionSummaryResponse res = managementService.updateUserAccess(100L, req);

        verify(userPermissionRepository).deleteByUserId(100L);
        verify(userPermissionRepository).flush();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserPermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(userPermissionRepository).saveAll(captor.capture());

        List<UserPermission> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).anyMatch(up -> up.getPermission().getCode().equals("COMPONENT_CREATE")
                && UserPermission.ACCESS_GRANT.equals(up.getAccessType()));
        assertThat(saved).anyMatch(up -> up.getPermission().getCode().equals("COMPONENT_VIEW")
                && UserPermission.ACCESS_REVOKE.equals(up.getAccessType()));

        assertThat(res.getGrantedPermissions()).containsExactly("COMPONENT_CREATE");
        assertThat(res.getRevokedPermissions()).containsExactly("COMPONENT_VIEW");
        assertThat(res.getEffectivePermissions()).extracting("code").containsExactly("COMPONENT_CREATE");
    }
}
