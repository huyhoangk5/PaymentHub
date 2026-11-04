package com.training.PaymentHub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration-ms:86400000}") // 1 ngày
    private long jwtAccessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 ngày
    private long jwtRefreshExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sinh Access Token và nhúng danh sách mã Permission vào Claim Payload
     */
    public String generateAccessToken(String username, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("permissions", permissions) // Đưa danh sách permissions vào JWT Payload
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Sinh Refresh Token (chỉ cần chứa Username, không mang theo permissions)
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("permissions", List.class);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}