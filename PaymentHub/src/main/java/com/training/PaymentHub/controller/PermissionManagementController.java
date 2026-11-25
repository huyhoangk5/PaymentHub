package com.training.PaymentHub.controller;

import com.training.PaymentHub.model.dto.request.UpdateUserAccessRequest;
import com.training.PaymentHub.model.dto.response.ApiResponse;
import com.training.PaymentHub.model.dto.response.PermissionCatalogResponse;
import com.training.PaymentHub.model.dto.response.UserPermissionSummaryResponse;
import com.training.PaymentHub.service.PermissionManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions-management")
@RequiredArgsConstructor
public class PermissionManagementController {

    private final PermissionManagementService permissionManagementService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserPermissionSummaryResponse>>> searchUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<UserPermissionSummaryResponse> result = permissionManagementService.searchUsers(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<PermissionCatalogResponse>> getCatalog() {
        return ResponseEntity.ok(ApiResponse.success(permissionManagementService.getCatalog()));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserPermissionSummaryResponse>> updateUserAccess(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UpdateUserAccessRequest request) {
        UserPermissionSummaryResponse updated = permissionManagementService.updateUserAccess(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}
