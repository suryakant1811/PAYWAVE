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
@Table(name = "wallets")
public class Wallet {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "owner_id", nullable = false, unique = true, length = 36)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private OwnerType ownerType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "per_txn_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal perTxnLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
    }

    public static Wallet open(UUID ownerId, OwnerType ownerType, String currency,
                              BigDecimal dailyLimit, BigDecimal monthlyLimit, BigDecimal perTxnLimit) {
        Wallet wallet = new Wallet();
        wallet.id = UUID.randomUUID();
        wallet.ownerId = ownerId;
        wallet.ownerType = ownerType;
        wallet.currency = currency;
        wallet.balance = new BigDecimal("0.00");
        wallet.dailyLimit = dailyLimit;
        wallet.monthlyLimit = monthlyLimit;
        wallet.perTxnLimit = perTxnLimit;
        wallet.status = WalletStatus.ACTIVE;
        Instant now = Instant.now();
        wallet.createdAt = now;
        wallet.updatedAt = now;
        return wallet;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public OwnerType getOwnerType() { return ownerType; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; this.updatedAt = Instant.now(); }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public BigDecimal getPerTxnLimit() { return perTxnLimit; }
    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
