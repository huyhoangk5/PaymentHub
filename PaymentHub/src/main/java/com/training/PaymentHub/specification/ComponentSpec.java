package com.training.PaymentHub.specification;

import com.training.PaymentHub.model.dto.request.ComponentSearchRequest;
import com.training.PaymentHub.model.entity.Component;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class ComponentSpec {
    public static Specification<Component> filter(ComponentSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // Component Code
            if (req.getComponentCode() != null && !req.getComponentCode().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("componentCode")), "%" + req.getComponentCode().trim().toLowerCase() + "%"));
            } else if (req.getComponentCodes() != null && !req.getComponentCodes().isEmpty()) {
                List<Predicate> codePreds = new ArrayList<>();
                for (String code : req.getComponentCodes()) {
                    if (code != null && !code.trim().isEmpty()) {
                        codePreds.add(cb.like(cb.lower(root.get("componentCode")), "%" + code.trim().toLowerCase() + "%"));
                    }
                }
                if (!codePreds.isEmpty()) {
                    predicates.add(cb.or(codePreds.toArray(new Predicate[0])));
                }
            }

            // Component Name
            if (req.getComponentName() != null && !req.getComponentName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("componentName")), "%" + req.getComponentName().trim().toLowerCase() + "%"));
            } else if (req.getComponentNames() != null && !req.getComponentNames().isEmpty()) {
                List<Predicate> namePreds = new ArrayList<>();
                for (String name : req.getComponentNames()) {
                    if (name != null && !name.trim().isEmpty()) {
                        namePreds.add(cb.like(cb.lower(root.get("componentName")), "%" + name.trim().toLowerCase() + "%"));
                    }
                }
                if (!namePreds.isEmpty()) {
                    predicates.add(cb.or(namePreds.toArray(new Predicate[0])));
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

            // Connection Method
            if (req.getConnectionMethod() != null && !req.getConnectionMethod().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("connectionMethod")), "%" + req.getConnectionMethod().trim().toLowerCase() + "%"));
            }

            // Check Token
            if (req.getCheckToken() != null && !req.getCheckToken().trim().isEmpty()) {
                boolean val = "true".equalsIgnoreCase(req.getCheckToken()) || "1".equals(req.getCheckToken()) || "Y".equalsIgnoreCase(req.getCheckToken()) || "Có".equalsIgnoreCase(req.getCheckToken());
                predicates.add(cb.equal(root.get("checkToken"), val));
            }

            // IsDisplay
            if (req.getIsDisplay() != null) {
                predicates.add(cb.equal(root.get("isDisplay"), req.getIsDisplay()));
            }

            // Date range
            if (req.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effectiveDate"), req.getFromDate()));
            }
            if (req.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effectiveDate"), req.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

// //Tìm theo mã cấu phần
// public static Specification<Component> findByComponentCode(List<String>
// componentCodes){
// if (componentCodes == null || componentCodes.isEmpty()) return null;
// return (root, query, cb) -> {
// return root.get("componentCode").in(componentCodes);
// };
// }
// //Tìm theo tên cấu phần
// public static Specification<Component> findByCoponentName(List<String>
// componentNames){
// if (componentNames == null || componentNames.isEmpty()) return null;
// return (root, query, cb) ->{
// return root.get("componentName").in(componentNames);
// };
// }
// //Tìm theo trạng thái tham số
// public static Specification<Component> findByStatus(List<Integer> statuses){
// if (statuses == null || statuses.isEmpty()) return null;
// return (root, query, cb) ->{
// return root.get("status").in(statuses);
// };
// }
// //Tìm theo trạng thái hoạt động
// public static Specification<Component> findByIsActive(List<Integer>
// isActives){
// if (isActives == null || isActives.isEmpty()) return null;
// return(root, query, cb) -> {
// return root.get("isActive").in(isActives);
// };
// }
