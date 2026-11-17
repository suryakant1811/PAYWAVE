package com.paywave.notification.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.api.PageResponse;
import com.paywave.notification.domain.NotificationMessage;
import com.paywave.notification.service.NotificationService;
import com.paywave.web.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications = notifications; }

    @io.swagger.v3.oas.annotations.Operation(summary = "List simulated email, SMS, and in-app notifications")
    @GetMapping("/api/v1/notifications")
    public ApiResponse<PageResponse<NotificationResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        var result = notifications.list(SecurityUtils.currentUserId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(new PageResponse<>(result.map(NotificationResponse::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/internal/events")
    public ApiResponse<Map<String, Integer>> accept(@RequestBody EventRequest request) {
        int created = notifications.accept(request.eventId(), request.eventType(), request.payload());
        return ApiResponse.ok(Map.of("created", created));
    }

    public record EventRequest(UUID eventId, String eventType, String aggregateType, String aggregateId, String occurredAt, JsonNode payload) {}

    public record NotificationResponse(UUID id, String channel, String eventType, String title, String body, String status, Instant createdAt) {
        static NotificationResponse from(NotificationMessage message) {
            return new NotificationResponse(message.getId(), message.getChannel(), message.getEventType(), message.getTitle(),
                    message.getBody(), message.getStatus(), message.getCreatedAt());
        }
    }
}
