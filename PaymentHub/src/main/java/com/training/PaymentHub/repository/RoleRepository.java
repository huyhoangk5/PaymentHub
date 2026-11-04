package com.training.PaymentHub.repository;

import com.training.PaymentHub.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);
}
