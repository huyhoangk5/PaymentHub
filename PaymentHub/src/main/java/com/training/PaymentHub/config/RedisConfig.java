package com.training.PaymentHub.config;

import com.training.PaymentHub.kafka.RedisNotificationSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    @Value("${app.redis.export-channel:export:notifications}")
    private String exportChannel;

    private final RedisNotificationSubscriber redisNotificationSubscriber;

    @Bean
    public ChannelTopic exportNotificationTopic() {
        return new ChannelTopic(exportChannel);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisNotificationSubscriber, exportNotificationTopic());
        return container;
    }
}

