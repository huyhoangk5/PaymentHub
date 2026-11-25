package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.request.UpdateUserAccessRequest;
import com.training.PaymentHub.model.dto.response.PermissionCatalogResponse;
import com.training.PaymentHub.model.dto.response.UserPermissionSummaryResponse;
import org.springframework.data.domain.Page;

public interface PermissionManagementService {

    Page<UserPermissionSummaryResponse> searchUsers(String keyword, int page, int size);

    PermissionCatalogResponse getCatalog();

    UserPermissionSummaryResponse updateUserAccess(Long userId, UpdateUserAccessRequest request);
}
