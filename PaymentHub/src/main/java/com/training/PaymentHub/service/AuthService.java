package com.training.PaymentHub.service;

import com.training.PaymentHub.model.dto.request.LoginRequest;
import com.training.PaymentHub.model.dto.request.LogoutRequest;
import com.training.PaymentHub.model.dto.request.RefreshTokenRequest;
import com.training.PaymentHub.model.dto.request.RegisterRequest;
import com.training.PaymentHub.model.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    TokenResponse login(LoginRequest req);

    TokenResponse refreshToken(RefreshTokenRequest req);

    void register(RegisterRequest req);

    void logout(LogoutRequest req, HttpServletRequest request);
}