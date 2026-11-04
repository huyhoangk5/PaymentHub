package com.training.PaymentHub.repository;

import com.training.PaymentHub.model.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(@NotBlank(message = "Tên đăng nhập không được để trống") @Size(min = 4, max = 50, message = "Tên đăng nhập từ 4-50 ký tự") String username);
}