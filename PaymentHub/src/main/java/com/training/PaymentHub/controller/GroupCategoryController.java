package com.training.PaymentHub.controller;

import com.training.PaymentHub.model.dto.request.GroupCategoryCreateRequest;
import com.training.PaymentHub.model.dto.request.GroupCategorySearchRequest;
import com.training.PaymentHub.model.dto.request.GroupCategoryUpdateRequest;
import com.training.PaymentHub.model.dto.response.ApiResponse;
import com.training.PaymentHub.model.dto.response.GroupCategoryResponse;
import com.training.PaymentHub.service.GroupCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/group-categories")
@RequiredArgsConstructor
public class GroupCategoryController {

    private final GroupCategoryService groupCategoryService;

    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_VIEW', 'COMPONENT_VIEW')")
    public ResponseEntity<ApiResponse<Page<GroupCategoryResponse>>> searchGroupCategories(@RequestBody GroupCategorySearchRequest req) {
        Page<GroupCategoryResponse> data = groupCategoryService.searchGroupCategory(req);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_CREATE', 'COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> createGroupCategory(
            @Valid @RequestBody GroupCategoryCreateRequest req,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        GroupCategoryResponse data = groupCategoryService.createGroupCategory(req, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_UPDATE', 'COMPONENT_UPDATE')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> updateGroupCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody GroupCategoryUpdateRequest req,
            Principal principal,
            HttpServletRequest request) {
        req.setId(id);
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        GroupCategoryResponse data = groupCategoryService.updateGroupCategory(req, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/copy/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_CREATE', 'COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> copyGroupCategory(
            @PathVariable("id") Long id,
            @RequestParam(value = "newParamName", required = false) String newParamName,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        GroupCategoryResponse data = groupCategoryService.copyGroupCategory(id, newParamName, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/send-approve/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_SEND', 'COMPONENT_SEND')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> sendApprove(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(groupCategoryService.sendApproveGroupCategory(id, userCode, clientIp)));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_APPROVE', 'COMPONENT_APPROVE')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> approve(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(groupCategoryService.approveGroupCategory(id, userCode, clientIp)));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_REJECT', 'COMPONENT_REJECT')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> reject(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(groupCategoryService.rejectGroupCategory(id, reason, userCode, clientIp)));
    }

    @PostMapping("/cancel/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_CANCEL', 'COMPONENT_CANCEL')")
    public ResponseEntity<ApiResponse<GroupCategoryResponse>> cancel(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(groupCategoryService.cancelApproveGroupCategory(id, userCode, clientIp)));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('GROUP_CATEGORY_CANCEL', 'GROUP_CATEGORY_CREATE', 'COMPONENT_CANCEL', 'COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        groupCategoryService.deleteGroupCategory(id, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String getUserCode(Principal principal) {
        return (principal != null) ? principal.getName() : "USER";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
