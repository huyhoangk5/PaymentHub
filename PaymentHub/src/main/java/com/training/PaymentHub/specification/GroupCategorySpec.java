package com.training.PaymentHub.specification;

import com.training.PaymentHub.model.dto.request.GroupCategorySearchRequest;
import com.training.PaymentHub.model.entity.GroupCategory;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class GroupCategorySpec {

    public static Specification<GroupCategory> filter(GroupCategorySearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Param Type
            if (req.getParamType() != null && !req.getParamType().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("paramType")), "%" + req.getParamType().trim().toLowerCase() + "%"));
            } else if (req.getParamTypes() != null && !req.getParamTypes().isEmpty()) {
                List<Predicate> pPreds = new ArrayList<>();
                for (String pt : req.getParamTypes()) {
                    if (pt != null && !pt.trim().isEmpty()) {
                        pPreds.add(cb.like(cb.lower(root.get("paramType")), "%" + pt.trim().toLowerCase() + "%"));
                    }
                }
                if (!pPreds.isEmpty()) {
                    predicates.add(cb.or(pPreds.toArray(new Predicate[0])));
                }
            }

            // Param Value
            if (req.getParamValue() != null && !req.getParamValue().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("paramValue")), "%" + req.getParamValue().trim().toLowerCase() + "%"));
            } else if (req.getParamValues() != null && !req.getParamValues().isEmpty()) {
                List<Predicate> vPreds = new ArrayList<>();
                for (String pv : req.getParamValues()) {
                    if (pv != null && !pv.trim().isEmpty()) {
                        vPreds.add(cb.like(cb.lower(root.get("paramValue")), "%" + pv.trim().toLowerCase() + "%"));
                    }
                }
                if (!vPreds.isEmpty()) {
                    predicates.add(cb.or(vPreds.toArray(new Predicate[0])));
                }
            }

            // Param Name
            if (req.getParamName() != null && !req.getParamName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("paramName")), "%" + req.getParamName().trim().toLowerCase() + "%"));
            } else if (req.getParamNames() != null && !req.getParamNames().isEmpty()) {
                List<Predicate> nPreds = new ArrayList<>();
                for (String pn : req.getParamNames()) {
                    if (pn != null && !pn.trim().isEmpty()) {
                        nPreds.add(cb.like(cb.lower(root.get("paramName")), "%" + pn.trim().toLowerCase() + "%"));
                    }
                }
                if (!nPreds.isEmpty()) {
                    predicates.add(cb.or(nPreds.toArray(new Predicate[0])));
                }
            }

            // Component Code
            if (req.getComponentCode() != null && !req.getComponentCode().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("componentCode")), "%" + req.getComponentCode().trim().toLowerCase() + "%"));
            } else if (req.getComponentCodes() != null && !req.getComponentCodes().isEmpty()) {
                List<Predicate> cPreds = new ArrayList<>();
                for (String cc : req.getComponentCodes()) {
                    if (cc != null && !cc.trim().isEmpty()) {
                        cPreds.add(cb.like(cb.lower(root.get("componentCode")), "%" + cc.trim().toLowerCase() + "%"));
                    }
                }
                if (!cPreds.isEmpty()) {
                    predicates.add(cb.or(cPreds.toArray(new Predicate[0])));
                }
            }

            // Status
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            } else if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(req.getStatuses()));
            }

            // IsActive
            if (req.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), req.getIsActive()));
            } else if (req.getIsActives() != null && !req.getIsActives().isEmpty()) {
                predicates.add(root.get("isActive").in(req.getIsActives()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

//    // Tìm theo Danh mục theo nhóm (PARAM_TYPE)
//    public static Specification<GroupCategory> findByParamType(List<String> paramTypes) {
//        if (paramTypes == null || paramTypes.isEmpty()) return null;
//        return (root, query, cb) -> root.get("paramType").in(paramTypes);
//    }
//
//    // Tìm theo Giá trị thành phần (PARAM_VALUE)
//    public static Specification<GroupCategory> findByParamValue(List<String> paramValues) {
//        if (paramValues == null || paramValues.isEmpty()) return null;
//        return (root, query, cb) -> root.get("paramValue").in(paramValues);
//    }
//
//    // Tìm theo Tên thành phần (PARAM_NAME)
//    public static Specification<GroupCategory> findByParamName(List<String> paramNames) {
//        if (paramNames == null || paramNames.isEmpty()) return null;
//        return (root, query, cb) -> root.get("paramName").in(paramNames);
//    }
//
//    // Tìm theo Mã cấu phần xử lý (COMPONENT_CODE)
//    public static Specification<GroupCategory> findByComponentCode(List<String> componentCodes) {
//        if (componentCodes == null || componentCodes.isEmpty()) return null;
//        return (root, query, cb) -> root.get("componentCode").in(componentCodes);
//    }
//
//    // Tìm theo Trạng thái tham số (STATUS: 1 - Mới, 3 - Chờ duyệt, 4 - Đã duyệt, 5 - Từ chối, 7 - Hủy duyệt)
//    public static Specification<GroupCategory> findByStatus(List<Integer> statuses) {
//        if (statuses == null || statuses.isEmpty()) return null;
//        return (root, query, cb) -> root.get("status").in(statuses);
//    }
//
//    // Tìm theo Trạng thái hoạt động (IS_ACTIVE: 1 - Hoạt động, 0 - Ngừng hoạt động)
//    public static Specification<GroupCategory> findByIsActive(List<Integer> isActives) {
//        if (isActives == null || isActives.isEmpty()) return null;
//        return (root, query, cb) -> root.get("isActive").in(isActives);
//    }
}