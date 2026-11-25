package com.training.PaymentHub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.PaymentHub.model.dto.ExportNotificationEvent;
import com.training.PaymentHub.model.dto.request.TransactionSearchRequest;
import com.training.PaymentHub.service.RedisProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisProgressServiceImpl implements RedisProgressService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.export-channel:export:notifications}")
    private String exportChannel;

    @Value("${app.redis.job-ttl-hours:24}")
    private long jobTtlHours;

    private static final String JOB_STATE_PREFIX = "export:job:";
    private static final String JOB_FILTER_PREFIX = "export:filter:";

    @Override
    public void saveJobState(ExportNotificationEvent state) {
        try {
            String key = JOB_STATE_PREFIX + state.getJobId();
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(jobTtlHours));
        } catch (Exception e) {
            log.warn("Không thể lưu trạng thái Job {} vào Redis: {}", state.getJobId(), e.getMessage());
        }
    }

    @Override
    public ExportNotificationEvent getJobState(String jobId) {
        try {
            String json = redisTemplate.opsForValue().get(JOB_STATE_PREFIX + jobId);
            if (json != null) {
                return objectMapper.readValue(json, ExportNotificationEvent.class);
            }
        } catch (Exception e) {
            log.warn("Không thể đọc trạng thái Job {} từ Redis: {}", jobId, e.getMessage());
        }
        return null;
    }

    @Override
    public void saveJobFilter(String jobId, TransactionSearchRequest filter) {
        try {
            String key = JOB_FILTER_PREFIX + jobId;
            String json = objectMapper.writeValueAsString(filter != null ? filter : new TransactionSearchRequest());
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(jobTtlHours));
        } catch (Exception e) {
            log.warn("Không thể lưu bộ lọc Job {} vào Redis: {}", jobId, e.getMessage());
        }
    }

    @Override
    public TransactionSearchRequest getJobFilter(String jobId) {
        TransactionSearchRequest filter = getJobFilterIfPresent(jobId);
        return filter != null ? filter : new TransactionSearchRequest();
    }

    @Override
    public TransactionSearchRequest getJobFilterIfPresent(String jobId) {
        try {
            String json = redisTemplate.opsForValue().get(JOB_FILTER_PREFIX + jobId);
            if (json != null) {
                return objectMapper.readValue(json, TransactionSearchRequest.class);
            }
        } catch (Exception e) {
            log.warn("Không thể đọc bộ lọc Job {} từ Redis: {}", jobId, e.getMessage());
        }
        return null;
    }

    @Override
    public void publishNotification(ExportNotificationEvent event) {
        try {
            saveJobState(event);
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(exportChannel, json);
            log.info("Đã phát sự kiện Redis Pub/Sub cho Job {}: status={}", event.getJobId(), event.getStatus());
        } catch (Exception e) {
            log.error("Lỗi phát sự kiện Redis Pub/Sub cho Job {}: {}", event.getJobId(), e.getMessage(), e);
        }
    }
}

