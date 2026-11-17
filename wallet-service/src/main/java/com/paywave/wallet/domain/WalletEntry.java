package com.paywave.wallet.domain;

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
@Table(name = "wallet_entries")
public class WalletEntry {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "wallet_id", nullable = false, length = 36)
    private UUID walletId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", nullable = false, length = 32)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reference_id", nullable = false, length = 36)
    private UUID referenceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WalletEntry() {
    }

    public static WalletEntry of(UUID walletId, String idempotencyKey, EntryType type, BigDecimal amount,
                                 BigDecimal balanceAfter, String referenceType, UUID referenceId) {
        WalletEntry entry = new WalletEntry();
        entry.id = UUID.randomUUID();
        entry.walletId = walletId;
        entry.idempotencyKey = idempotencyKey;
        entry.type = type;
        entry.amount = amount;
        entry.balanceAfter = balanceAfter;
        entry.referenceType = referenceType;
        entry.referenceId = referenceId;
        entry.createdAt = Instant.now();
        return entry;
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public EntryType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public Instant getCreatedAt() { return createdAt; }
}
