package com.paywave.web.outbox;

import com.paywave.common.messaging.EventTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class RoutingOutboxDispatcher implements OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(RoutingOutboxDispatcher.class);

    private final String mode;
    private final RestClient notificationClient;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;

    public RoutingOutboxDispatcher(String mode,
                                   RestClient notificationClient,
                                   ObjectMapper objectMapper,
                                   ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate) {
        this.mode = mode == null ? "log" : mode;
        this.notificationClient = notificationClient;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void dispatch(OutboxEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", event.getId());
        body.put("eventType", event.getEventType());
        body.put("aggregateType", event.getAggregateType());
        body.put("aggregateId", event.getAggregateId());
        body.put("occurredAt", event.getCreatedAt() == null ? Instant.now().toString() : event.getCreatedAt().toString());
        body.put("payload", readPayload(event.getPayload()));
        switch (mode) {
            case "log" -> log.info("Domain event {} {} {}", event.getEventType(), event.getAggregateType(), event.getAggregateId());
            case "http" -> notificationClient.post()
                    .uri("/internal/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            case "kafka" -> {
                KafkaTemplate<String, String> template = kafkaTemplate.getIfAvailable();
                if (template == null) {
                    throw new IllegalStateException("Kafka mode is enabled but no KafkaTemplate bean is available");
                }
                try {
                    template.send(EventTypes.TOPIC, event.getEventType(), objectMapper.writeValueAsString(body)).get();
                } catch (Exception ex) {
                    throw new IllegalStateException("Kafka publish failed", ex);
                }
            }
            default -> throw new IllegalStateException("Unknown messaging mode " + mode);
        }
    }

    private Object readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            return payload;
        }
    }

    public static RoutingOutboxDispatcher create(String mode,
                                                 String notificationUrl,
                                                 com.paywave.web.http.InternalRestClientFactory factory,
                                                 ObjectMapper objectMapper,
                                                 ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate) {
        String base = notificationUrl == null || notificationUrl.isBlank() ? "http://localhost:8081" : notificationUrl;
        return new RoutingOutboxDispatcher(mode, factory.create(base), objectMapper, kafkaTemplate);
    }
}
