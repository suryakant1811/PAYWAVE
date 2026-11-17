package com.paywave.refund.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {
    @Id @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length = 36, nullable = false) private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "payment_id", nullable = false, length = 36) private UUID paymentId;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "requester_id", nullable = false, length = 36) private UUID requesterId;
    @Column(name = "idempotency_key", nullable = false, length = 80) private String idempotencyKey;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "fee_component", nullable = false, precision = 19, scale = 2) private BigDecimal feeComponent;
    @Column(name = "net_component", nullable = false, precision = 19, scale = 2) private BigDecimal netComponent;
    @Column(nullable = false, length = 255) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RefundStatus status;
    @Column(name = "failure_reason", length = 255) private String failureReason;
    @Column(name = "payment_type", nullable = false, length = 20) private String paymentType;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "payer_id", nullable = false, length = 36) private UUID payerId;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "payee_id", nullable = false, length = 36) private UUID payeeId;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "notify_user_id", length = 36) private UUID notifyUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Refund() {}

    public static Refund request(UUID paymentId, UUID requesterId, String idempotencyKey, BigDecimal amount, BigDecimal feeComponent,
                                 BigDecimal netComponent, String reason, String paymentType, UUID payerId, UUID payeeId, UUID notifyUserId) {
        Refund refund = new Refund();
        refund.id = UUID.randomUUID();
        refund.paymentId = paymentId;
        refund.requesterId = requesterId;
        refund.idempotencyKey = idempotencyKey;
        refund.amount = amount;
        refund.feeComponent = feeComponent;
        refund.netComponent = netComponent;
        refund.reason = reason;
        refund.status = RefundStatus.REQUESTED;
        refund.paymentType = paymentType;
        refund.payerId = payerId;
        refund.payeeId = payeeId;
        refund.notifyUserId = notifyUserId;
        Instant now = Instant.now();
        refund.createdAt = now;
        refund.updatedAt = now;
        return refund;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getRequesterId() { return requesterId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFeeComponent() { return feeComponent; }
    public BigDecimal getNetComponent() { return netComponent; }
    public String getReason() { return reason; }
    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; this.updatedAt = Instant.now(); }
    public String getPaymentType() { return paymentType; }
    public UUID getPayerId() { return payerId; }
    public UUID getPayeeId() { return payeeId; }
    public UUID getNotifyUserId() { return notifyUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
