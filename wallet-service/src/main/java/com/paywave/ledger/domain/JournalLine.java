package com.paywave.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "journal_lines")
public class JournalLine {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "journal_entry_id", nullable = false, length = 36)
    private UUID journalEntryId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "account_id", nullable = false, length = 36)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected JournalLine() {
    }

    public static JournalLine of(UUID journalEntryId, UUID accountId, Direction direction, BigDecimal amount, String currency) {
        JournalLine line = new JournalLine();
        line.id = UUID.randomUUID();
        line.journalEntryId = journalEntryId;
        line.accountId = accountId;
        line.direction = direction;
        line.amount = amount;
        line.currency = currency;
        return line;
    }

    @PreUpdate
    @PreRemove
    public void denyMutation() {
        throw new UnsupportedOperationException("Posted journal lines are immutable");
    }

    public UUID getId() { return id; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public UUID getAccountId() { return accountId; }
    public Direction getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
