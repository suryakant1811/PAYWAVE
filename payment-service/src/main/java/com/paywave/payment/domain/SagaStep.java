package com.paywave.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_saga_steps")
public class SagaStep {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_id", nullable = false, length = 36)
    private UUID paymentId;

    @Column(name = "step_name", nullable = false, length = 64)
    private String stepName;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SagaStep() {
    }

    public static SagaStep of(UUID paymentId, String stepName, String status, String detail) {
        SagaStep step = new SagaStep();
        step.id = UUID.randomUUID();
        step.paymentId = paymentId;
        step.stepName = stepName;
        step.status = status;
        step.detail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
        step.createdAt = Instant.now();
        return step;
    }
}
