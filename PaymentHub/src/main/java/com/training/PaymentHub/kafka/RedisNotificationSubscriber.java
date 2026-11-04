package com.training.PaymentHub.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.PaymentHub.model.dto.ExportNotificationEvent;
import com.training.PaymentHub.websocket.ExportWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ExportWebSocketHandler webSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            ExportNotificationEvent event = objectMapper.readValue(json, ExportNotificationEvent.class);
            log.info("Nhận sự kiện Redis Pub/Sub cho Job {}: status={}, userId={}",
                    event.getJobId(), event.getStatus(), event.getUserId());

            webSocketHandler.sendToUser(event.getUserId(), json);
        } catch (Exception e) {
            log.error("Lỗi xử lý sự kiện Redis Pub/Sub: {}", e.getMessage(), e);
        }
    }
}

