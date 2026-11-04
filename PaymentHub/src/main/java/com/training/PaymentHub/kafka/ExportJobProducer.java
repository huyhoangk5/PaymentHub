package com.training.PaymentHub.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.PaymentHub.model.dto.ExportJobKafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportJobProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.export-topic:export_jobs}")
    private String exportTopic;

    public void sendExportJob(ExportJobKafkaMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(exportTopic, message.getJobId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Gửi Kafka message thất bại cho Job {}: {}", message.getJobId(), ex.getMessage(), ex);
                        } else {
                            log.info("Đã đẩy Export Job {} vào Kafka topic {} (partition={}, offset={})",
                                    message.getJobId(),
                                    exportTopic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Lỗi serialize Kafka message cho Job {}: {}", message.getJobId(), e.getMessage(), e);
            throw new RuntimeException("Không thể đẩy yêu cầu xuất file vào hàng đợi xử lý", e);
        }
    }
}

