package com.paywave.web.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE attempts < 8
              AND (status = 'PENDING'
                   OR (status = 'IN_PROGRESS' AND updated_at < (UTC_TIMESTAMP(3) - INTERVAL 60 SECOND)))
            ORDER BY created_at
            LIMIT 25
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch();
}
