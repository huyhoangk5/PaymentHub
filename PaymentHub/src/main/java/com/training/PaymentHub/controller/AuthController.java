package com.training.PaymentHub.controller;

import com.training.PaymentHub.model.dto.request.LoginRequest;
import com.training.PaymentHub.model.dto.request.RefreshTokenRequest;
import com.training.PaymentHub.model.dto.response.ApiResponse;
import com.training.PaymentHub.model.dto.response.TokenResponse;
import com.training.PaymentHub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(req)));
    }
}