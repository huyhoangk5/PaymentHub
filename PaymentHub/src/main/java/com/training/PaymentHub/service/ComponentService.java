package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.request.ComponentCreateRequest;
import com.training.PaymentHub.model.dto.request.ComponentSearchRequest;
import com.training.PaymentHub.model.dto.request.ComponentUpdateRequest;
import com.training.PaymentHub.model.dto.response.ComponentResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ComponentService {
    Page<ComponentResponse> searchComponent(ComponentSearchRequest req);

    Map<String, List<String>> getDistinctOptions();

    ComponentResponse createComponent(ComponentCreateRequest req, String userCode, String ipAddress);

    ComponentResponse updateComponent(ComponentUpdateRequest req, String userCode, String ipAddress);

    ComponentResponse copyComponent(Long id, String componentCode, String userCode, String ipAddress);

    ComponentResponse sendApproveComponent(Long id, String userCode, String ipAddress);

    ComponentResponse approveComponent(Long id, String userCode, String ipAddress);

    ComponentResponse rejectComponent(Long id, String reason, String userCode, String ipAddress);

    ComponentResponse cancelApproveComponent(Long id, String userCode, String ipAddress);

    void deleteComponent(Long id, String userCode, String ipAddress);
}
