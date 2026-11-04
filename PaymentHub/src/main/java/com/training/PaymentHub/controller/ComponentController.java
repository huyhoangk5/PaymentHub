package com.training.PaymentHub.controller;

import com.training.PaymentHub.model.dto.request.ComponentCreateRequest;
import com.training.PaymentHub.model.dto.request.ComponentSearchRequest;
import com.training.PaymentHub.model.dto.request.ComponentUpdateRequest;
import com.training.PaymentHub.model.dto.response.ApiResponse;
import com.training.PaymentHub.model.dto.response.ComponentResponse;
import com.training.PaymentHub.service.ComponentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController {

    private final ComponentService componentService;

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
    public ResponseEntity<ApiResponse<Page<ComponentResponse>>> searchComponents(@RequestBody ComponentSearchRequest req) {
        Page<ComponentResponse> data = componentService.searchComponent(req);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getDistinctOptions() {
        Map<String, List<String>> data = componentService.getDistinctOptions();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<ComponentResponse>> createComponent(
            @Valid @RequestBody ComponentCreateRequest req,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        ComponentResponse data = componentService.createComponent(req, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_UPDATE')")
    public ResponseEntity<ApiResponse<ComponentResponse>> updateComponent(
            @PathVariable("id") Long id,
            @Valid @RequestBody ComponentUpdateRequest req,
            Principal principal,
            HttpServletRequest request) {
        req.setId(id);
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        ComponentResponse data = componentService.updateComponent(req, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/copy/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<ComponentResponse>> copyComponent(
            @PathVariable("id") Long id,
            @RequestParam(value = "newComponentCode", required = false) String newComponentCode,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        ComponentResponse data = componentService.copyComponent(id, newComponentCode, userCode, clientIp);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/send-approve/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_SEND')")
    public ResponseEntity<ApiResponse<ComponentResponse>> sendApprove(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(componentService.sendApproveComponent(id, userCode, clientIp)));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_APPROVE')")
    public ResponseEntity<ApiResponse<ComponentResponse>> approve(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(componentService.approveComponent(id, userCode, clientIp)));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_REJECT')")
    public ResponseEntity<ApiResponse<ComponentResponse>> reject(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(componentService.rejectComponent(id, reason, userCode, clientIp)));
    }

    @PostMapping("/cancel/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_CANCEL')")
    public ResponseEntity<ApiResponse<ComponentResponse>> cancel(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(ApiResponse.success(componentService.cancelApproveComponent(id, userCode, clientIp)));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('COMPONENT_CANCEL', 'COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long id,
            Principal principal,
            HttpServletRequest request) {
        String userCode = getUserCode(principal);
        String clientIp = getClientIp(request);
        componentService.deleteComponent(id, userCode, clientIp);
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
