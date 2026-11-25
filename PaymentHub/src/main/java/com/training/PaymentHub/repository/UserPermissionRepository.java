package com.training.PaymentHub.repository;

import com.training.PaymentHub.model.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    @Query("SELECT up FROM UserPermission up JOIN FETCH up.permission WHERE up.user.id = :userId")
    List<UserPermission> findByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserPermission up JOIN FETCH up.permission WHERE up.user.id IN :userIds")
    List<UserPermission> findByUserIdIn(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
