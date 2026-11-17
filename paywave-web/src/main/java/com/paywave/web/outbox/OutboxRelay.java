package com.paywave.web.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final OutboxDispatcher dispatcher;
    private final TransactionTemplate transactionTemplate;

    public OutboxRelay(OutboxEventRepository repository,
                       OutboxDispatcher dispatcher,
                       PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.dispatcher = dispatcher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void publishPending() {
        List<UUID> claimed = transactionTemplate.execute(status -> claimIds());
        if (claimed == null) {
            return;
        }
        for (UUID id : claimed) {
            try {
                OutboxEvent event = transactionTemplate.execute(status -> repository.findById(id).orElseThrow());
                dispatcher.dispatch(event);
                transactionTemplate.executeWithoutResult(status -> markPublished(id));
            } catch (RuntimeException ex) {
                log.warn("Outbox publish failed for {}: {}", id, ex.getMessage());
                transactionTemplate.executeWithoutResult(status -> markRetry(id, ex.getMessage()));
            }
        }
    }

    private List<UUID> claimIds() {
        List<OutboxEvent> events = repository.lockNextBatch();
        Instant now = Instant.now();
        for (OutboxEvent event : events) {
            event.setStatus("IN_PROGRESS");
            event.setUpdatedAt(now);
        }
        return events.stream().map(OutboxEvent::getId).toList();
    }

    private void markPublished(UUID id) {
        repository.findById(id).ifPresent(event -> {
            Instant now = Instant.now();
            event.setStatus("PUBLISHED");
            event.setPublishedAt(now);
            event.setUpdatedAt(now);
            event.setLastError(null);
        });
    }

    private void markRetry(UUID id, String error) {
        repository.findById(id).ifPresent(event -> {
            event.setStatus("PENDING");
            event.setAttempts(event.getAttempts() + 1);
            event.setUpdatedAt(Instant.now());
            event.setLastError(error == null ? null : error.substring(0, Math.min(error.length(), 500)));
        });
    }
}
