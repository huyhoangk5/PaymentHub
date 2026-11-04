package com.training.PaymentHub.repository;

import com.training.PaymentHub.model.entity.GroupCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupCategoryRepository extends 
        JpaRepository<GroupCategory, Long>, 
        JpaSpecificationExecutor<GroupCategory> {

    // Kiểm tra trùng ParamName trong cùng ParamType khi thêm mới hoặc sửa (Check trùng theo RSD)[cite: 4]
    boolean existsByParamTypeAndParamName(String paramType, String paramName);

    boolean existsByParamTypeAndParamNameAndIdNot(String paramType, String paramName, Long id);

    // Kiểm tra trùng ParamValue trong cùng ParamType
    boolean existsByParamTypeAndParamValue(String paramType, String paramValue);

    boolean existsByParamTypeAndParamValueAndIdNot(String paramType, String paramValue, Long id);
}