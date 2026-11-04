package com.training.PaymentHub.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.training.PaymentHub.model.dto.request.LoginRequest;
import com.training.PaymentHub.model.dto.request.LogoutRequest;
import com.training.PaymentHub.model.dto.request.RefreshTokenRequest;
import com.training.PaymentHub.model.dto.request.RegisterRequest;
import com.training.PaymentHub.model.dto.response.TokenResponse;
import com.training.PaymentHub.model.entity.Role;
import com.training.PaymentHub.model.entity.User;
import com.training.PaymentHub.repository.RoleRepository;
import com.training.PaymentHub.repository.UserRepository;
import com.training.PaymentHub.security.JwtTokenProvider;
import com.training.PaymentHub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Cache đếm số lần đăng nhập sai ngay trong RAM (Tự xóa key sau 5 phút không hoạt động)
    private final Cache<String, Integer> failedAttemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    @Transactional
    public void register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống!");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName().trim());

        // Gán Role mặc định khi Đăng ký (Ví dụ: ROLE_VIEWER)
        Role defaultRole = roleRepository.findByRoleCode("ROLE_VIEWER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò mặc định ROLE_VIEWER"));
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest req) {
        String username = req.getUsername().trim();

        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        // 1. KIỂM TRA TRẠNG THÁI KHÓA TẠM THỜI
        if (user.getLockoutUntil() != null) {
            if (LocalDateTime.now().isBefore(user.getLockoutUntil())) {
                long secondsLeft = Duration.between(LocalDateTime.now(), user.getLockoutUntil()).getSeconds();
                throw new RuntimeException("Tài khoản tạm thời bị khóa do nhập sai nhiều lần. Vui lòng thử lại sau " + secondsLeft + " giây!");
            } else {
                user.setLockoutUntil(null);
                userRepository.save(user);
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, req.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Tăng bộ đếm thất bại +1
            int attempts = failedAttemptsCache.get(username, k -> 0) + 1;
            failedAttemptsCache.put(username, attempts);

            // Nếu sai đủ 5 lần -> Cập nhật LOCKOUT_UNTIL vào DB (Khóa 30 giây)
            if (attempts >= 5) {
                user.setLockoutUntil(LocalDateTime.now().plusSeconds(30));
                userRepository.save(user);
                failedAttemptsCache.invalidate(username);

                throw new RuntimeException("Bạn đã nhập sai mật khẩu 5 lần liên tiếp. Tài khoản bị khóa trong 30 giây!");
            }

            int attemptsLeft = 5 - attempts;
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác. Bạn còn " + attemptsLeft + " lần thử!");
        }

        // 3. ĐĂNG NHẬP THÀNH CÔNG
        failedAttemptsCache.invalidate(username);
        if (user.getLockoutUntil() != null) {
            user.setLockoutUntil(null);
        }

        // Gom toàn bộ Permission Code từ tất cả Roles
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());
        user.getRoles().forEach(role -> permissions.add(role.getRoleCode()));
        enrichGroupCategoryPermissions(permissions);

        String accessToken = tokenProvider.generateAccessToken(user.getUsername(), permissions);
        String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        // Lưu Refresh Token vào Database
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .permissions(permissions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refreshToken(RefreshTokenRequest req) {
        if (tokenProvider.validateToken(req.getRefreshToken())) {
            String username = tokenProvider.getUsernameFromJWT(req.getRefreshToken());

            User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));

            List<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getCode())
                    .distinct()
                    .collect(Collectors.toList());
            user.getRoles().forEach(role -> permissions.add(role.getRoleCode()));
            enrichGroupCategoryPermissions(permissions);

            String newAccessToken = tokenProvider.generateAccessToken(username, permissions);

            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(req.getRefreshToken())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .permissions(permissions)
                    .build();
        }
        throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã hết hạn");
    }

    private void enrichGroupCategoryPermissions(List<String> permissions) {
        List<String> snapshot = new ArrayList<>(permissions);
        for (String perm : snapshot) {
            if (perm != null && perm.startsWith("COMPONENT_")) {
                String groupCatPerm = perm.replace("COMPONENT_", "GROUP_CATEGORY_");
                if (!permissions.contains(groupCatPerm)) {
                    permissions.add(groupCatPerm);
                }
            }
        }
    }

    @Override
    @Transactional
    public void logout(LogoutRequest req, HttpServletRequest request) {
        // Lấy Access Token từ Header Request
        String bearerToken = request.getHeader("Authorization");
        String accessToken = null;
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
        }

        // Lấy Username người thực hiện
        String username = (accessToken != null) ? tokenProvider.getUsernameFromJWT(accessToken) : "UNKNOWN";

        // Xóa Refresh Token lưu trong Database
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
        });
    }
}
