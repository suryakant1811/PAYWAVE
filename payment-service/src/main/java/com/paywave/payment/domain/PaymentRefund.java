package com.paywave.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refunds")
public class PaymentRefund {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_id", nullable = false, length = 36)
    private UUID paymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentRefund() {
    }

    public static PaymentRefund of(UUID refundId, UUID paymentId, BigDecimal amount) {
        PaymentRefund refund = new PaymentRefund();
        refund.id = refundId;
        refund.paymentId = paymentId;
        refund.amount = amount;
        refund.createdAt = Instant.now();
        return refund;
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
}
