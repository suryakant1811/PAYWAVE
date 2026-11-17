package com.paywave.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationMessage {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;
    @Column(nullable = false, length = 16)
    private String channel;
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 500)
    private String body;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "dedup_key", nullable = false, unique = true, length = 120)
    private String dedupKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationMessage() {}

    public static NotificationMessage sent(UUID userId, String channel, String eventType, String title, String body, String dedupKey) {
        NotificationMessage message = new NotificationMessage();
        message.id = UUID.randomUUID();
        message.userId = userId;
        message.channel = channel;
        message.eventType = eventType;
        message.title = title;
        message.body = body;
        message.status = "SENT";
        message.dedupKey = dedupKey;
        message.createdAt = Instant.now();
        return message;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
