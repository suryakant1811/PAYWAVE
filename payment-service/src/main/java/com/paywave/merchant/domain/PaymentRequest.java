package com.paywave.merchant.domain;

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
@Table(name = "payment_requests")
public class PaymentRequest {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "merchant_id", nullable = false, length = 36)
    private UUID merchantId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(length = 255)
    private String note;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentRequest() {}

    public static PaymentRequest create(UUID merchantId, BigDecimal amount, String note, Instant expiresAt) {
        PaymentRequest request = new PaymentRequest();
        request.id = UUID.randomUUID();
        request.merchantId = merchantId;
        request.amount = amount;
        request.note = note;
        request.expiresAt = expiresAt;
        request.createdAt = Instant.now();
        return request;
    }

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getNote() { return note; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
