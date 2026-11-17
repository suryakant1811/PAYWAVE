package com.paywave.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "ledger_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_ledger_account", columnNames = {"owner_id", "account_type", "currency"}))
public class LedgerAccount {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "owner_id", nullable = false, length = 36)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_side", nullable = false, length = 10)
    private NormalSide normalSide;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerAccount() {
    }

    public static LedgerAccount open(UUID ownerId, AccountType accountType, String currency) {
        LedgerAccount account = new LedgerAccount();
        account.id = UUID.randomUUID();
        account.ownerId = ownerId;
        account.accountType = accountType;
        account.currency = currency;
        account.normalSide = accountType == AccountType.GATEWAY_CLEARING ? NormalSide.DEBIT : NormalSide.CREDIT;
        account.createdAt = Instant.now();
        return account;
    }

    @PreUpdate
    @PreRemove
    public void denyMutation() {
        throw new UnsupportedOperationException("Ledger accounts are append-only");
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public AccountType getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public NormalSide getNormalSide() { return normalSide; }
    public Instant getCreatedAt() { return createdAt; }
}
