package com.training.PaymentHub.websocket;

import com.training.PaymentHub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String ATTR_USER_ID = "USER_ID";

    /**
     * Quản lý danh sách kết nối WebSocket theo từng UserId (hỗ trợ mở nhiều tab trình duyệt).
     */
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = authenticateSession(session);
        if (!StringUtils.hasText(userId)) {
            log.warn("Từ chối kết nối WebSocket do JWT không hợp lệ: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        session.getAttributes().put(ATTR_USER_ID, userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userIdObj = session.getAttributes().get(ATTR_USER_ID);
        if (userIdObj instanceof String userId) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            log.info("WebSocket disconnected: userId={}, sessionId={}", userId, session.getId());
        }
    }

    /**
     * Đẩy thông báo JSON tới đúng người dùng sở hữu Export Job (User Isolation).
     */
    public void sendToUser(String userId, String jsonPayload) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("User {} hiện không có kết nối WebSocket active (offline).", userId);
            return;
        }

        TextMessage message = new TextMessage(jsonPayload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.error("Lỗi khi gửi WebSocket tới user {} (session {}): {}", userId, session.getId(), e.getMessage());
                }
            }
        }
    }

    private String authenticateSession(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return null;
            MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
            String token = queryParams.getFirst("token");
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUsernameFromJWT(token);
            }
        } catch (Exception e) {
            log.error("Lỗi xác thực JWT WebSocket: {}", e.getMessage());
        }
        return null;
    }
}

