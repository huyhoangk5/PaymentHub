package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.request.GroupCategoryCreateRequest;
import com.training.PaymentHub.model.dto.request.GroupCategorySearchRequest;
import com.training.PaymentHub.model.dto.request.GroupCategoryUpdateRequest;
import com.training.PaymentHub.model.dto.response.GroupCategoryResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public interface GroupCategoryService {

    Page<GroupCategoryResponse> searchGroupCategory(GroupCategorySearchRequest req);

    GroupCategoryResponse createGroupCategory(GroupCategoryCreateRequest req, String userCode, String clientIp);

    GroupCategoryResponse updateGroupCategory(GroupCategoryUpdateRequest req, String userCode, String clientIp);

    GroupCategoryResponse copyGroupCategory(Long id, String newParamName, String userCode, String ipAddress);

    GroupCategoryResponse sendApproveGroupCategory(Long id, String userCode, String ipAddress);

    GroupCategoryResponse approveGroupCategory(Long id, String userCode, String ipAddress);

    GroupCategoryResponse rejectGroupCategory(Long id, String reason, String userCode, String ipAddress);

    GroupCategoryResponse cancelApproveGroupCategory(Long id, String userCode, String ipAddress);

    void deleteGroupCategory(Long id, String userCode, String ipAddress);
}
