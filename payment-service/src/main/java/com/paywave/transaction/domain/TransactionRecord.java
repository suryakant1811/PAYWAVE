package com.paywave.transaction.domain;

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
@Table(name = "transactions")
public class TransactionRecord {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_id", nullable = false, length = 36)
    private UUID paymentId;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "owner_id", nullable = false, length = 36)
    private UUID ownerId;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "counterparty_id", length = 36)
    private UUID counterpartyId;
    @Column(name = "counterparty_name", length = 120)
    private String counterpartyName;
    @Column(nullable = false, length = 10)
    private String direction;
    @Column(name = "txn_type", nullable = false, length = 32)
    private String type;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, length = 255)
    private String description;
    @Column(name = "reference_type", nullable = false, length = 32)
    private String referenceType;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected TransactionRecord() {}

    public static TransactionRecord create(UUID paymentId, UUID ownerId, UUID counterpartyId, String counterpartyName,
                                           String direction, String type, BigDecimal amount, String status,
                                           String description, String referenceType) {
        TransactionRecord record = new TransactionRecord();
        record.id = UUID.randomUUID();
        record.paymentId = paymentId;
        record.ownerId = ownerId;
        record.counterpartyId = counterpartyId;
        record.counterpartyName = counterpartyName;
        record.direction = direction;
        record.type = type;
        record.amount = amount;
        record.currency = "INR";
        record.status = status;
        record.description = description;
        record.referenceType = referenceType;
        record.occurredAt = Instant.now();
        return record;
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getCounterpartyId() { return counterpartyId; }
    public String getCounterpartyName() { return counterpartyName; }
    public String getDirection() { return direction; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public String getReferenceType() { return referenceType; }
    public Instant getOccurredAt() { return occurredAt; }
}
