package com.paywave.notification.service;

import com.paywave.common.messaging.EventTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.paywave.notification.domain.NotificationMessage;
import com.paywave.notification.repo.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String[] CHANNELS = {"IN_APP", "EMAIL", "SMS"};
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) { this.notifications = notifications; }

    @Transactional
    public int accept(UUID eventId, String eventType, JsonNode payload) {
        if (payload == null || !payload.has("userIds") || !payload.get("userIds").isArray()) {
            return 0;
        }
        String title = title(eventType);
        String body = body(eventType, payload);
        int created = 0;
        for (JsonNode userNode : payload.get("userIds")) {
            UUID userId = UUID.fromString(userNode.asText());
            for (String channel : CHANNELS) {
                String dedup = eventId + ":" + userId + ":" + channel;
                if (notifications.findByDedupKey(dedup).isPresent()) {
                    continue;
                }
                try {
                    notifications.saveAndFlush(NotificationMessage.sent(userId, channel, eventType, title, body, dedup));
                    log.info("Simulated {} to user {} for {}: {}", channel, userId, eventType, body);
                    created++;
                } catch (DataIntegrityViolationException ignored) {
                    // Another delivery of the same event already stored this channel.
                }
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public Page<NotificationMessage> list(UUID userId, Pageable pageable) {
        return notifications.findByUserId(userId, pageable);
    }

    static String title(String eventType) {
        return switch (eventType) {
            case EventTypes.PAYMENT_COMPLETED -> "Payment successful";
            case EventTypes.PAYMENT_FAILED -> "Payment failed";
            case EventTypes.WALLET_CREDITED -> "Wallet credited";
            case EventTypes.WALLET_DEBITED -> "Wallet debited";
            case EventTypes.REFUND_COMPLETED -> "Refund completed";
            case EventTypes.SETTLEMENT_COMPLETED -> "Settlement completed";
            default -> "PayWave update";
        };
    }

    static String body(String eventType, JsonNode payload) {
        String amount = payload.path("amount").asText("");
        String balance = payload.path("balanceAfter").asText("");
        return switch (eventType) {
            case EventTypes.PAYMENT_COMPLETED -> "Your payment of INR " + amount + " was successful.";
            case EventTypes.PAYMENT_FAILED -> "Your payment of INR " + amount + " could not be completed.";
            case EventTypes.WALLET_CREDITED -> "INR " + amount + " was credited to your wallet. Balance INR " + balance + ".";
            case EventTypes.WALLET_DEBITED -> "INR " + amount + " was debited from your wallet. Balance INR " + balance + ".";
            case EventTypes.REFUND_COMPLETED -> "A refund of INR " + amount + " was processed.";
            case EventTypes.SETTLEMENT_COMPLETED -> "Merchant settlement of INR " + amount + " was completed.";
            default -> "You have a new PayWave notification.";
        };
    }
}
