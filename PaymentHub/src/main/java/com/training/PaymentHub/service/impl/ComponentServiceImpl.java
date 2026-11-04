package com.training.PaymentHub.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.data.domain.PageImpl;
import tools.jackson.databind.ObjectMapper;
import com.training.PaymentHub.constant.CommonStatus;
import com.training.PaymentHub.model.dto.request.ComponentCreateRequest;
import com.training.PaymentHub.model.dto.request.ComponentSearchRequest;
import com.training.PaymentHub.model.dto.request.ComponentUpdateRequest;
import com.training.PaymentHub.model.dto.response.ComponentResponse;
import com.training.PaymentHub.model.entity.Component;
import com.training.PaymentHub.repository.ComponentRepository;
import com.training.PaymentHub.service.AuditLogService;
import com.training.PaymentHub.service.ComponentService;
import com.training.PaymentHub.specification.ComponentSpec;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ComponentServiceImpl implements ComponentService {

    private static final String MODULE_NAME = "COMPONENT";

    private final ComponentRepository componentRepository;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    public ComponentServiceImpl(ComponentRepository componentRepository, AuditLogService auditLogService) {
        this.componentRepository = componentRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public Page<ComponentResponse> searchComponent(ComponentSearchRequest req) {
        // Dùng spec
        Specification<Component> spec = ComponentSpec.filter(req);

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        Page<Component> page = componentRepository.findAll(spec, pageable);

        // Bỏ qua các dòng null (bản ghi dữ liệu không hợp lệ, ví dụ ID null trong DB) để tránh NPE
        List<ComponentResponse> content = page.getContent().stream()
                .filter(Objects::nonNull)
                .map(this::mapToRespond)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());

        // // Dùng procedure
        // // 1. Tạo StoredProcedureQuery gọi Package Procedure
        // StoredProcedureQuery query =
        // entityManager.createStoredProcedureQuery("PMH_COMPONENTS_PKG.SP_SEARCH_COMPONENTS",
        // Component.class);

        // // 2. Khai báo các tham số đầu vào (IN) và đầu ra (OUT SYS_REFCURSOR)
        // query.registerStoredProcedureParameter("p_componentCode", String.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_componentName", String.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_checkToken", String.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_status", Integer.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_isActive", Integer.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("P_connectionMethod", String.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_isDisplay", Integer.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_fromDate", java.sql.Date.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_toDate", java.sql.Date.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_page_no", Integer.class,
        // ParameterMode.IN);
        // query.registerStoredProcedureParameter("p_page_size", Integer.class,
        // ParameterMode.IN);

        // query.registerStoredProcedureParameter("p_cursor", void.class,
        // ParameterMode.REF_CURSOR);

        // // 3. Truyền giá trị vào Procedure (Map từ ComponentSearchRequest)
        // // Chú ý: Trang ở FE thường bắt đầu từ 0 (Spring Page 0-based), nhưng
        // Procedure
        // // của bạn dùng (p_page_no - 1) * p_page_size + 1 nên p_page_no truyền vào
        // nên
        // // >= 1.
        // int pageNo = (req.getPage() <= 0) ? 1 : req.getPage() + 1;
        // int pageSize = (req.getSize() <= 0) ? 10 : req.getSize();

        // query.setParameter("p_componentCode", req.getComponentCode());
        // query.setParameter("p_componentName", req.getComponentName());
        // query.setParameter("p_checkToken", req.getCheckToken());
        // query.setParameter("p_status", req.getStatus());
        // query.setParameter("p_isActive", req.getIsActive());
        // query.setParameter("P_connectionMethod", req.getConnectionMethod());
        // query.setParameter("p_isDisplay", req.getIsDisplay());
        // query.setParameter("p_fromDate", req.getFromDate() != null ?
        // java.sql.Date.valueOf(req.getFromDate()) : null);
        // query.setParameter("p_toDate", req.getToDate() != null ?
        // java.sql.Date.valueOf(req.getToDate()) : null);
        // query.setParameter("p_page_no", pageNo);
        // query.setParameter("p_page_size", pageSize);

        // // 4. Thực thi procedure và lấy danh sách kết quả
        // query.execute();
        // @SuppressWarnings("unchecked")
        // List<Component> componentList = query.getResultList();

        // // 5. Tính tổng số bản ghi (Total Elements)
        // // Nếu danh sách không rỗng, ta có thể lấy Total Elements bằng câu Native
        // Query
        // // nhanh hoặc thêm tham số OUT ở procedure
        // long totalElements = (long) query.getOutputParameterValue("p_total_count");

        // // 6. Map sang Page<ComponentResponse>
        // Page<Component> pageResult = new PageImpl<>(componentList,
        // PageRequest.of(req.getPage(), pageSize),
        // totalElements);
        // return pageResult.map(this::mapToRespond);
    }

    @Override
    public Map<String, List<String>> getDistinctOptions() {
        List<String> messageTypes = splitAndDistinct(componentRepository.findDistinctMessageTypes());
        List<String> connectionMethods = splitAndDistinct(componentRepository.findDistinctConnectionMethods());
        List<String> componentCodes = splitAndDistinct(componentRepository.findDistinctComponentCodes());
        List<String> componentNames = componentRepository.findDistinctComponentNames().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, List<String>> options = new HashMap<>();
        options.put("messageTypes", messageTypes);
        options.put("connectionMethods", connectionMethods);
        options.put("componentCodes", componentCodes);
        options.put("componentNames", componentNames);
        return options;
    }

    private List<String> splitAndDistinct(List<String> rawValues) {
        if (rawValues == null) {
            return List.of();
        }
        return rawValues.stream()
                .filter(Objects::nonNull)
                .flatMap(val -> Arrays.stream(val.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComponentResponse createComponent(ComponentCreateRequest req, String userCode, String ipAddress) {
        if (componentRepository.existsByComponentCode(req.getComponentCode().trim())) {
            throw new RuntimeException("Mã cấu phần [" + req.getComponentCode() + "] đã tồn tại!");
        }

        if (req.getEndEffectiveDate() != null && req.getEffectiveDate() != null
                && req.getEndEffectiveDate().isBefore(req.getEffectiveDate())) {
            throw new RuntimeException("Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực!");
        }

        Component component = new Component();
        component.setComponentCode(req.getComponentCode().trim());
        component.setComponentName(req.getComponentName().trim());
        component.setMessageType(req.getMessageType() != null ? req.getMessageType().trim() : null);
        component.setConnectionMethod(req.getConnectionMethod() != null ? req.getConnectionMethod().trim() : null);
        component.setEffectiveDate(req.getEffectiveDate());
        component.setEndEffectiveDate(req.getEndEffectiveDate());
        component.setNewData(null);

        component.setCheckToken(req.getCheckToken() != null ? req.getCheckToken() : false);
        component.setStatus(CommonStatus.NEW.getCode());
        component.setIsActive(1);
        component.setIsDisplay(1);

        Component savedComponent = componentRepository.save(component);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Tạo bản ghi",
                userCode,
                "Tạo mới Tham số " + savedComponent.getComponentName(),
                null,
                savedComponent.getStatus(),
                ipAddress,
                null,
                toJson(req));

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public ComponentResponse updateComponent(ComponentUpdateRequest req, String userCode, String ipAddress) {
        Component updateComponent = componentRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần với ID [" + req.getId() + "]"));

        if (componentRepository.existsByComponentCodeAndIdNot(req.getComponentCode().trim(), req.getId())) {
            throw new RuntimeException(
                    "Mã cấu phần [" + req.getComponentCode() + "] đã được sử dụng bởi cấu phần khác!");
        }

        if (req.getEndEffectiveDate() != null && req.getEffectiveDate() != null
                && req.getEndEffectiveDate().isBefore(req.getEffectiveDate())) {
            throw new RuntimeException("Ngày hết hiệu lực phải lớn hơn hoặc bằng ngày hiệu lực!");
        }

        int statusBefore = updateComponent.getStatus();
        String oldDataJson = toJson(updateComponent);
        String newDataJson = toJson(req);

        // Nếu bản ghi ĐÃ DUYỆT (status == 4) -> Lưu dữ liệu thay đổi vào newData[cite:
        // 3, 4]
        if (updateComponent.getStatus() == CommonStatus.APPROVED.getCode()) {
            updateComponent.setNewData(newDataJson);
            updateComponent.setStatus(CommonStatus.NEW.getCode());
        } else {
            updateComponent.setComponentCode(req.getComponentCode().trim());
            updateComponent.setComponentName(req.getComponentName().trim());
            updateComponent.setMessageType(req.getMessageType() != null ? req.getMessageType().trim() : null);
            updateComponent.setConnectionMethod(req.getConnectionMethod() != null ? req.getConnectionMethod().trim() : null);
            updateComponent.setEffectiveDate(req.getEffectiveDate());
            updateComponent.setEndEffectiveDate(req.getEndEffectiveDate());
            updateComponent.setCheckToken(req.getCheckToken() != null ? req.getCheckToken() : false);
        }

        Component savedComponent = componentRepository.save(updateComponent);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Sửa",
                userCode,
                "Sửa Tham số " + savedComponent.getComponentName(),
                statusBefore,
                savedComponent.getStatus(),
                ipAddress,
                oldDataJson,
                newDataJson);

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public ComponentResponse copyComponent(Long id, String newComponentCode, String userCode, String ipAddress) {
        if (newComponentCode == null || newComponentCode.trim().isEmpty()) {
            throw new RuntimeException("Mã cấu phần mới không được để trống");
        }

        Component sourceComponent = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần gốc để sao chép"));

        if (componentRepository.existsByComponentCode(newComponentCode.trim())) {
            throw new RuntimeException("Mã cấu phần [" + newComponentCode + "] đã tồn tại");
        }

        Component copyComponent = new Component();
        copyComponent.setComponentCode(newComponentCode.trim());
        copyComponent.setComponentName(sourceComponent.getComponentName().trim());
        copyComponent.setMessageType(sourceComponent.getMessageType() != null ? sourceComponent.getMessageType().trim() : null);
        copyComponent.setConnectionMethod(sourceComponent.getConnectionMethod() != null ? sourceComponent.getConnectionMethod().trim() : null);
        copyComponent.setEffectiveDate(sourceComponent.getEffectiveDate());
        copyComponent.setEndEffectiveDate(sourceComponent.getEndEffectiveDate());
        copyComponent.setCheckToken(sourceComponent.getCheckToken() != null ? sourceComponent.getCheckToken() : false);

        copyComponent.setStatus(CommonStatus.NEW.getCode());
        copyComponent.setIsActive(1);
        copyComponent.setIsDisplay(1);

        Component savedComponent = componentRepository.save(copyComponent);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Sao chép bản ghi",
                userCode,
                "Sao chép từ Tham số [id=" + id + "]",
                null,
                savedComponent.getStatus(),
                ipAddress,
                null,
                toJson(savedComponent));

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public ComponentResponse sendApproveComponent(Long id, String userCode, String ipAddress) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần"));

        if (component.getStatus() != 1 && component.getStatus() != 5 && component.getStatus() != 7) {
            throw new RuntimeException("Cấu phần này không thể gửi duyệt");
        }

        int statusBefore = component.getStatus();
        component.setStatus(3);

        Component savedComponent = componentRepository.save(component);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Gửi duyệt",
                userCode,
                "Gửi duyệt Tham số " + savedComponent.getComponentName() + " [id=" + id + "]",
                statusBefore,
                savedComponent.getStatus(),
                ipAddress,
                null,
                null);

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public ComponentResponse approveComponent(Long id, String userCode, String ipAddress) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần"));

        if (component.getStatus() != 3) {
            throw new RuntimeException("Cấu phần này không thể duyệt");
        }

        int statusBefore = component.getStatus();
        String oldDataJson = toJson(component);

        if (component.getNewData() != null && !component.getNewData().trim().isEmpty()) {
            try {
                ComponentUpdateRequest updateData = objectMapper.readValue(component.getNewData(),
                        ComponentUpdateRequest.class);

                component.setComponentCode(updateData.getComponentCode());
                component.setComponentName(updateData.getComponentName());
                component.setMessageType(updateData.getMessageType());
                component.setConnectionMethod(updateData.getConnectionMethod());
                component.setEffectiveDate(updateData.getEffectiveDate());
                component.setEndEffectiveDate(updateData.getEndEffectiveDate());
                if (updateData.getCheckToken() != null) {
                    component.setCheckToken(updateData.getCheckToken());
                }

                component.setNewData(null);
            } catch (Exception e) {
                throw new RuntimeException("Có lỗi trong quá trình đồng bộ dữ liệu từ newData: " + e.getMessage());
            }
        }

        component.setStatus(4);
        component.setIsDisplay(2);
        Component savedComponent = componentRepository.save(component);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Phê duyệt",
                userCode,
                "Phê duyệt Tham số " + savedComponent.getComponentName(),
                statusBefore,
                savedComponent.getStatus(),
                ipAddress,
                oldDataJson,
                toJson(savedComponent));

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public ComponentResponse rejectComponent(Long id, String reason, String userCode, String ipAddress) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần"));

        if (component.getStatus() != 3) {
            throw new RuntimeException("Cấu phần này không thể từ chối duyệt");
        }

        int statusBefore = component.getStatus();

        if (component.getNewData() != null) {
            component.setNewData(null);
        }

        component.setStatus(5);

        Component savedComponent = componentRepository.save(component);

        String desc = "Từ chối duyệt Tham số " + savedComponent.getComponentName();
        if (reason != null && !reason.trim().isEmpty()) {
            desc += ". Lý do: " + reason;
        }

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Từ chối",
                userCode,
                desc,
                statusBefore,
                savedComponent.getStatus(),
                ipAddress,
                null,
                null);

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public ComponentResponse cancelApproveComponent(Long id, String userCode, String ipAddress) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần"));

        if (component.getIsDisplay() == 1
                && (component.getStatus() == 1 || component.getStatus() == 5)) {
            ComponentResponse response = mapToRespond(component);
            deleteComponent(id, userCode, ipAddress);
            return response;
        }

        if (component.getStatus() != 3 && component.getStatus() != 4) {
            throw new RuntimeException("Cấu phần này không thể hủy duyệt");
        }

        int statusBefore = component.getStatus();

        if (component.getStatus() == 3 && component.getNewData() != null) {
            component.setNewData(null);
            component.setStatus(4);
        } else {
            component.setNewData(null);
            component.setStatus(7);
        }

        Component savedComponent = componentRepository.save(component);

        // Ghi Audit Log
        auditLogService.saveLog(
                MODULE_NAME,
                savedComponent.getId().toString(),
                "Hủy phê duyệt",
                userCode,
                "Hủy phê duyệt Tham số " + savedComponent.getComponentName(),
                statusBefore,
                savedComponent.getStatus(),
                ipAddress,
                null,
                null);

        return mapToRespond(savedComponent);
    }

    @Override
    @Transactional
    public void deleteComponent(Long id, String userCode, String ipAddress) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu phần"));

        if (component.getIsDisplay() == 2) {
            throw new RuntimeException("Không được phép xóa bản ghi đã từng được phê duyệt!");
        }
        if (component.getStatus() != 1 && component.getStatus() != 5) {
            throw new RuntimeException("Chỉ được phép xóa bản ghi ở trạng thái Mới hoặc Từ chối!");
        }

        String oldDataJson = toJson(component);
        int statusBefore = component.getStatus();
        String componentName = component.getComponentName();

        componentRepository.delete(component);

        auditLogService.saveLog(
                MODULE_NAME,
                id.toString(),
                "Xóa bản ghi",
                userCode,
                "Xóa Tham số " + componentName,
                statusBefore,
                null,
                ipAddress,
                oldDataJson,
                null);
    }

    private ComponentResponse mapToRespond(Component component) {
        return ComponentResponse.builder()
                .id(component.getId())
                .componentCode(component.getComponentCode())
                .componentName(component.getComponentName())
                .messageType(component.getMessageType())
                .connectionMethod(component.getConnectionMethod())
                .checkToken(component.getCheckToken())
                .status(CommonStatus.fromCode(component.getStatus()))
                .effectiveDate(component.getEffectiveDate())
                .endEffectiveDate(component.getEndEffectiveDate())
                .isActive(component.getIsActive())
                .isDispllay(component.getIsDisplay())
                .newData(component.getNewData())
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
