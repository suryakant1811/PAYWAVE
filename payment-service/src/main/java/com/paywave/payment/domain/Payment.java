package com.paywave.payment.domain;

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
@Table(name = "payments")
public class Payment {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payer_id", nullable = false, length = 36)
    private UUID payerId;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payee_id", nullable = false, length = 36)
    private UUID payeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payee_type", nullable = false, length = 20)
    private PayeeType payeeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "gateway_reference", length = 64)
    private String gatewayReference;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private boolean settled;

    @Column(name = "settled_at")
    private Instant settledAt;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "settlement_id", length = 36)
    private UUID settlementId;

    @Column(name = "history_recorded", nullable = false)
    private boolean historyRecorded;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "notify_user_id", length = 36)
    private UUID notifyUserId;

    @Column(name = "counterparty_name", length = 120)
    private String counterpartyName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public static Payment create(UUID payerId, String idempotencyKey, UUID payeeId, PayeeType payeeType, PaymentType type,
                                 BigDecimal amount, BigDecimal feeAmount, BigDecimal netAmount, String note,
                                 UUID notifyUserId, String counterpartyName) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.payerId = payerId;
        payment.idempotencyKey = idempotencyKey;
        payment.payeeId = payeeId;
        payment.payeeType = payeeType;
        payment.type = type;
        payment.amount = amount;
        payment.feeAmount = feeAmount;
        payment.netAmount = netAmount;
        payment.refundedAmount = new BigDecimal("0.00");
        payment.currency = "INR";
        payment.status = PaymentStatus.CREATED;
        payment.note = note;
        payment.settled = false;
        payment.historyRecorded = false;
        payment.notifyUserId = notifyUserId;
        payment.counterpartyName = counterpartyName;
        Instant now = Instant.now();
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    public UUID getId() { return id; }
    public UUID getPayerId() { return payerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getPayeeId() { return payeeId; }
    public PayeeType getPayeeType() { return payeeType; }
    public PaymentType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; touch(); }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; touch(); }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; touch(); }
    public String getGatewayReference() { return gatewayReference; }
    public void setGatewayReference(String gatewayReference) { this.gatewayReference = gatewayReference; touch(); }
    public String getNote() { return note; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; touch(); }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public UUID getSettlementId() { return settlementId; }
    public void setSettlementId(UUID settlementId) { this.settlementId = settlementId; }
    public boolean isHistoryRecorded() { return historyRecorded; }
    public void setHistoryRecorded(boolean historyRecorded) { this.historyRecorded = historyRecorded; touch(); }
    public UUID getNotifyUserId() { return notifyUserId; }
    public String getCounterpartyName() { return counterpartyName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private void touch() { this.updatedAt = Instant.now(); }
}
