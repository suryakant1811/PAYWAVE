package com.paywave.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "reference_type", nullable = false, length = 32)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reference_id", nullable = false, length = 36)
    private UUID referenceId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reversal_of", length = 36)
    private UUID reversalOf;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    protected JournalEntry() {
    }

    public static JournalEntry post(String idempotencyKey, String referenceType, UUID referenceId, UUID reversalOf, String description) {
        JournalEntry entry = new JournalEntry();
        entry.id = UUID.randomUUID();
        entry.idempotencyKey = idempotencyKey;
        entry.referenceType = referenceType;
        entry.referenceId = referenceId;
        entry.reversalOf = reversalOf;
        entry.description = description;
        entry.postedAt = Instant.now();
        return entry;
    }

    @PreUpdate
    @PreRemove
    public void denyMutation() {
        throw new UnsupportedOperationException("Posted journal entries are immutable");
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public UUID getReversalOf() { return reversalOf; }
    public String getDescription() { return description; }
    public Instant getPostedAt() { return postedAt; }
}
