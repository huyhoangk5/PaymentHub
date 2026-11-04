package com.training.PaymentHub.repository;

import com.training.PaymentHub.model.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComponentRepository extends
        JpaRepository<Component, Long>,
        JpaSpecificationExecutor<Component> {
    boolean existsByComponentCode(String componentCode);

    boolean existsByComponentCodeAndIdNot(String componentCode, Long id);

    Optional<Component> findById(Long id);

    @Query("SELECT DISTINCT c.messageType FROM Component c WHERE c.messageType IS NOT NULL")
    List<String> findDistinctMessageTypes();

    @Query("SELECT DISTINCT c.connectionMethod FROM Component c WHERE c.connectionMethod IS NOT NULL")
    List<String> findDistinctConnectionMethods();

    @Query("SELECT DISTINCT c.componentCode FROM Component c WHERE c.componentCode IS NOT NULL")
    List<String> findDistinctComponentCodes();

    @Query("SELECT DISTINCT c.componentName FROM Component c WHERE c.componentName IS NOT NULL")
    List<String> findDistinctComponentNames();

    @Procedure(procedureName = "PMH_COMPONENTS_PKG.SP_SEARCH_COMPONENTS")
    List<Component> searchComponentsProcedure(
            @Param("p_componentCode") String componentCode,
            @Param("p_componentName") String componentName,
            @Param("p_checkToken") String checkToken,
            @Param("p_status") Integer status,
            @Param("p_isActive") Integer isActive,
            @Param("P_connectionMethod") String connectionMethod,
            @Param("p_isDisplay") Integer isDisplay,
            @Param("p_fromDate") LocalDate fromDate,
            @Param("p_toDate") LocalDate toDate,
            @Param("p_page_no") Integer pageNo,
            @Param("p_page_size") Integer pageSize
    );
}
