package com.training.PaymentHub.security;

import com.training.PaymentHub.model.entity.User;
import com.training.PaymentHub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        // 1. Thêm Roles
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getRoleCode())));

        // 2. Thêm Permissions
        user.getRoles().forEach(role -> 
            role.getPermissions().forEach(permission -> 
                authorities.add(new SimpleGrantedAuthority(permission.getCode()))
            )
        );

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                "Y".equalsIgnoreCase(user.getEnabled()),
                true, true, true,
                authorities
        );
    }
}