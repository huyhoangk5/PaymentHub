package com.training.PaymentHub.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.training.PaymentHub.constant.CommonStatus;
import com.training.PaymentHub.model.dto.request.GroupCategoryCreateRequest;
import com.training.PaymentHub.model.dto.request.GroupCategorySearchRequest;
import com.training.PaymentHub.model.dto.request.GroupCategoryUpdateRequest;
import com.training.PaymentHub.model.dto.response.GroupCategoryResponse;
import com.training.PaymentHub.model.entity.GroupCategory;
import com.training.PaymentHub.repository.GroupCategoryRepository;
import com.training.PaymentHub.service.AuditLogService;
import com.training.PaymentHub.service.GroupCategoryService;
import com.training.PaymentHub.specification.GroupCategorySpec;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class GroupCategoryServiceImpl implements GroupCategoryService {

    private static final String MODULE_NAME = "GROUP_CATEGORY";

    private final GroupCategoryRepository groupCategoryRepository;
    private final AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    public GroupCategoryServiceImpl(GroupCategoryRepository groupCategoryRepository, AuditLogService auditLogService) {
        this.groupCategoryRepository = groupCategoryRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public Page<GroupCategoryResponse> searchGroupCategory(GroupCategorySearchRequest req) {
        Specification<GroupCategory> spec = GroupCategorySpec.filter(req);

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        Page<GroupCategory> page = groupCategoryRepository.findAll(spec, pageable);

        return page.map(this::mapToRespond);
    }


    @Override
    @Transactional
    public GroupCategoryResponse createGroupCategory(GroupCategoryCreateRequest req, String userCode, String ipAddress) {
        if (groupCategoryRepository.existsByParamTypeAndParamName(req.getParamType().trim(), req.getParamName().trim())) {
            throw new RuntimeException("Tên tham số [" + req.getParamName() + "] đã tồn tại trong nhóm [" + req.getParamType() + "]!");
        }

        if (req.getEndEffectiveDate() != null && req.getEffectiveDate() != null
                && req.getEndEffectiveDate().isBefore(req.getEffectiveDate())) {
            throw new RuntimeException("Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực!");
        }

        GroupCategory entity = new GroupCategory();
        entity.setParamName(req.getParamName().trim());
        entity.setParamValue(req.getParamValue() != null ? req.getParamValue().trim() : null);
        entity.setParamType(req.getParamType().trim());
        entity.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        entity.setComponentCode(req.getComponentCode() != null ? req.getComponentCode().trim() : null);
        entity.setEffectiveDate(req.getEffectiveDate());
        entity.setEndEffectiveDate(req.getEndEffectiveDate());
        entity.setNewData(null);

        entity.setStatus(CommonStatus.NEW.getCode());
        entity.setIsActive(1);
        entity.setIsDisplay(1);

        GroupCategory savedEntity = groupCategoryRepository.save(entity);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Tạo bản ghi",
                userCode,
                "Tạo mới Nhóm danh mục " + savedEntity.getParamName(),
                null,
                savedEntity.getStatus(),
                ipAddress,
                null,
                toJson(req)
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public GroupCategoryResponse updateGroupCategory(GroupCategoryUpdateRequest req, String userCode, String ipAddress) {
        GroupCategory updateEntity = groupCategoryRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục với ID [" + req.getId() + "]"));

        if (groupCategoryRepository.existsByParamTypeAndParamNameAndIdNot(req.getParamType().trim(), req.getParamName().trim(), req.getId())) {
            throw new RuntimeException("Tên tham số [" + req.getParamName() + "] đã tồn tại trong nhóm [" + req.getParamType() + "]!");
        }

        if (req.getEndEffectiveDate() != null && req.getEffectiveDate() != null
                && req.getEndEffectiveDate().isBefore(req.getEffectiveDate())) {
            throw new RuntimeException("Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực!");
        }

        int statusBefore = updateEntity.getStatus();
        String oldDataJson = toJson(updateEntity);
        String newDataJson = toJson(req);

        // Nếu bản ghi ĐÃ DUYỆT (status == 4) -> Lưu dữ liệu thay đổi vào newData
        if (updateEntity.getStatus() == CommonStatus.APPROVED.getCode()) {
            updateEntity.setNewData(newDataJson);
            updateEntity.setStatus(CommonStatus.NEW.getCode());
        } else {
            updateEntity.setParamName(req.getParamName().trim());
            updateEntity.setParamValue(req.getParamValue() != null ? req.getParamValue().trim() : null);
            updateEntity.setParamType(req.getParamType().trim());
            updateEntity.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
            updateEntity.setComponentCode(req.getComponentCode() != null ? req.getComponentCode().trim() : null);
            updateEntity.setEffectiveDate(req.getEffectiveDate());
            updateEntity.setEndEffectiveDate(req.getEndEffectiveDate());
        }

        GroupCategory savedEntity = groupCategoryRepository.save(updateEntity);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Sửa",
                userCode,
                "Sửa Nhóm danh mục " + savedEntity.getParamName(),
                statusBefore,
                savedEntity.getStatus(),
                ipAddress,
                oldDataJson,
                newDataJson
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public GroupCategoryResponse copyGroupCategory(Long id, String newParamName, String userCode, String ipAddress) {
        if (newParamName == null || newParamName.trim().isEmpty()) {
            throw new RuntimeException("Tên tham số mới không được để trống");
        }

        GroupCategory sourceEntity = groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục gốc để sao chép"));

        if (groupCategoryRepository.existsByParamTypeAndParamName(sourceEntity.getParamType(), newParamName.trim())) {
            throw new RuntimeException("Tên tham số [" + newParamName + "] đã tồn tại trong nhóm [" + sourceEntity.getParamType() + "]");
        }

        GroupCategory copyEntity = new GroupCategory();
        copyEntity.setParamName(newParamName.trim());
        copyEntity.setParamValue(sourceEntity.getParamValue());
        copyEntity.setParamType(sourceEntity.getParamType());
        copyEntity.setDescription(sourceEntity.getDescription());
        copyEntity.setComponentCode(sourceEntity.getComponentCode());
        copyEntity.setEffectiveDate(sourceEntity.getEffectiveDate());
        copyEntity.setEndEffectiveDate(sourceEntity.getEndEffectiveDate());

        copyEntity.setStatus(CommonStatus.NEW.getCode());
        copyEntity.setIsActive(1);
        copyEntity.setIsDisplay(1);

        GroupCategory savedEntity = groupCategoryRepository.save(copyEntity);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Sao chép bản ghi",
                userCode,
                "Sao chép từ Nhóm danh mục [id=" + id + "]",
                null,
                savedEntity.getStatus(),
                ipAddress,
                null,
                toJson(savedEntity)
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public GroupCategoryResponse sendApproveGroupCategory(Long id, String userCode, String ipAddress) {
        GroupCategory entity = groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục"));

        if (entity.getStatus() != 1 && entity.getStatus() != 5 && entity.getStatus() != 7) {
            throw new RuntimeException("Nhóm danh mục này không thể gửi duyệt");
        }

        int statusBefore = entity.getStatus();
        entity.setStatus(3);

        GroupCategory savedEntity = groupCategoryRepository.save(entity);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Gửi duyệt",
                userCode,
                "Gửi duyệt Nhóm danh mục " + savedEntity.getParamName() + " [id=" + id + "]",
                statusBefore,
                savedEntity.getStatus(),
                ipAddress,
                null,
                null
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public GroupCategoryResponse approveGroupCategory(Long id, String userCode, String ipAddress) {
        GroupCategory entity = groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục"));

        if (entity.getStatus() != 3) {
            throw new RuntimeException("Nhóm danh mục này không thể duyệt");
        }

        int statusBefore = entity.getStatus();
        String oldDataJson = toJson(entity);

        if (entity.getNewData() != null && !entity.getNewData().trim().isEmpty()) {
            try {
                GroupCategoryUpdateRequest updateData = objectMapper.readValue(entity.getNewData(), GroupCategoryUpdateRequest.class);

                entity.setParamName(updateData.getParamName());
                entity.setParamValue(updateData.getParamValue());
                entity.setParamType(updateData.getParamType());
                entity.setDescription(updateData.getDescription());
                entity.setComponentCode(updateData.getComponentCode());
                entity.setEffectiveDate(updateData.getEffectiveDate());
                entity.setEndEffectiveDate(updateData.getEndEffectiveDate());

                entity.setNewData(null);
            } catch (Exception e) {
                throw new RuntimeException("Có lỗi trong quá trình đồng bộ dữ liệu từ newData: " + e.getMessage());
            }
        }

        entity.setStatus(4);
        entity.setIsDisplay(2);
        GroupCategory savedEntity = groupCategoryRepository.save(entity);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Phê duyệt",
                userCode,
                "Phê duyệt Nhóm danh mục " + savedEntity.getParamName(),
                statusBefore,
                savedEntity.getStatus(),
                ipAddress,
                oldDataJson,
                toJson(savedEntity)
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public GroupCategoryResponse rejectGroupCategory(Long id, String reason, String userCode, String ipAddress) {
        GroupCategory entity = groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục"));

        if (entity.getStatus() != 3) {
            throw new RuntimeException("Nhóm danh mục này không thể từ chối duyệt");
        }

        int statusBefore = entity.getStatus();

        if (entity.getNewData() != null) {
            entity.setNewData(null);
        }

        entity.setStatus(5);

        GroupCategory savedEntity = groupCategoryRepository.save(entity);

        String desc = "Từ chối duyệt Nhóm danh mục " + savedEntity.getParamName();
        if (reason != null && !reason.trim().isEmpty()) {
            desc += ". Lý do: " + reason;
        }

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Từ chối",
                userCode,
                desc,
                statusBefore,
                savedEntity.getStatus(),
                ipAddress,
                null,
                null
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public GroupCategoryResponse cancelApproveGroupCategory(Long id, String userCode, String ipAddress) {
        GroupCategory entity = groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục"));

        if (entity.getIsDisplay() != null && entity.getIsDisplay() == 1
                && (entity.getStatus() == 1 || entity.getStatus() == 5)) {
            GroupCategoryResponse response = mapToRespond(entity);
            deleteGroupCategory(id, userCode, ipAddress);
            return response;
        }

        if (entity.getStatus() != 3 && entity.getStatus() != 4) {
            throw new RuntimeException("Nhóm danh mục này không thể hủy duyệt");
        }

        int statusBefore = entity.getStatus();

        if (entity.getStatus() == 3 && entity.getNewData() != null) {
            entity.setNewData(null);
            entity.setStatus(4);
        } else {
            entity.setNewData(null);
            entity.setStatus(7);
        }

        GroupCategory savedEntity = groupCategoryRepository.save(entity);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedEntity.getId().toString(),
                "Hủy phê duyệt",
                userCode,
                "Hủy phê duyệt Nhóm danh mục " + savedEntity.getParamName(),
                statusBefore,
                savedEntity.getStatus(),
                ipAddress,
                null,
                null
        );

        return mapToRespond(savedEntity);
    }

    @Override
    @Transactional
    public void deleteGroupCategory(Long id, String userCode, String ipAddress) {
        GroupCategory entity = groupCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm danh mục"));

        if (entity.getIsDisplay() != null && entity.getIsDisplay() == 2) {
            throw new RuntimeException("Không được phép xóa bản ghi đã từng được phê duyệt!");
        }
        if (entity.getStatus() != 1 && entity.getStatus() != 5) {
            throw new RuntimeException("Chỉ được phép xóa bản ghi ở trạng thái Mới hoặc Từ chối!");
        }

        String oldDataJson = toJson(entity);
        int statusBefore = entity.getStatus();
        String paramName = entity.getParamName();

        groupCategoryRepository.delete(entity);

        auditLogService.saveLog(
                MODULE_NAME,
                id.toString(),
                "Xóa bản ghi",
                userCode,
                "Xóa Nhóm danh mục " + paramName,
                statusBefore,
                null,
                ipAddress,
                oldDataJson,
                null
        );
    }

    private GroupCategoryResponse mapToRespond(GroupCategory entity) {
        return GroupCategoryResponse.builder()
                .id(entity.getId())
                .paramName(entity.getParamName())
                .paramValue(entity.getParamValue())
                .paramType(entity.getParamType())
                .description(entity.getDescription())
                .componentCode(entity.getComponentCode())
                .status(CommonStatus.fromCode(entity.getStatus()))
                .effectiveDate(entity.getEffectiveDate())
                .endEffectiveDate(entity.getEndEffectiveDate())
                .isActive(entity.getIsActive())
                .isDisplay(entity.getIsDisplay())
                .newData(entity.getNewData())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
