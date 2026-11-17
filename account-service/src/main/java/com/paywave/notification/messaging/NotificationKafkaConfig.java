package com.paywave.notification.messaging;

import com.paywave.common.messaging.EventTypes;
import com.paywave.notification.service.NotificationService;
import com.paywave.notification.web.NotificationController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "paywave.messaging.mode", havingValue = "kafka")
public class NotificationKafkaConfig {

    @Bean
    ConcurrentMessageListenerContainer<String, String> notificationContainer(
            @Value("${paywave.messaging.kafka.bootstrap-servers:localhost:9092}") String servers,
            NotificationService notifications,
            ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "paywave-notification");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ContainerProperties properties = new ContainerProperties(EventTypes.TOPIC);
        properties.setMessageListener((MessageListener<String, String>) record -> {
            try {
                NotificationController.EventRequest event = objectMapper.readValue(record.value(), NotificationController.EventRequest.class);
                notifications.accept(event.eventId(), event.eventType(), event.payload());
            } catch (Exception ex) {
                throw new IllegalStateException("Could not consume domain event", ex);
            }
        });
        ConcurrentMessageListenerContainer<String, String> container =
                new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(config), properties);
        container.setBeanName("paywaveNotificationContainer");
        return container;
    }
}
