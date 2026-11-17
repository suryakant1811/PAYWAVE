package com.paywave.settlement.domain;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length = 36, nullable = false) private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "merchant_id", nullable = false, length = 36) private UUID merchantId;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "owner_user_id", nullable = false, length = 36) private UUID ownerUserId;
    @Column(name = "period_date", nullable = false) private LocalDate periodDate;
    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2) private BigDecimal grossAmount;
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2) private BigDecimal feeAmount;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2) private BigDecimal refundAmount;
    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2) private BigDecimal netAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SettlementStatus status;
    @Column(name = "ledger_posted", nullable = false) private boolean ledgerPosted;
    @Column(name = "wallet_debited", nullable = false) private boolean walletDebited;
    @Column(name = "failure_reason", length = 255) private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Settlement() {}

    public static Settlement start(UUID merchantId, UUID ownerUserId, LocalDate periodDate, BigDecimal gross, BigDecimal fees, BigDecimal refunds, BigDecimal net) {
        Settlement settlement = new Settlement();
        settlement.id = UUID.randomUUID();
        settlement.merchantId = merchantId;
        settlement.ownerUserId = ownerUserId;
        settlement.periodDate = periodDate;
        settlement.grossAmount = gross;
        settlement.feeAmount = fees;
        settlement.refundAmount = refunds;
        settlement.netAmount = net;
        settlement.status = SettlementStatus.PROCESSING;
        settlement.ledgerPosted = false;
        settlement.walletDebited = false;
        Instant now = Instant.now();
        settlement.createdAt = now;
        settlement.updatedAt = now;
        return settlement;
    }

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public LocalDate getPeriodDate() { return periodDate; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public boolean isLedgerPosted() { return ledgerPosted; }
    public void setLedgerPosted(boolean ledgerPosted) { this.ledgerPosted = ledgerPosted; this.updatedAt = Instant.now(); }
    public boolean isWalletDebited() { return walletDebited; }
    public void setWalletDebited(boolean walletDebited) { this.walletDebited = walletDebited; this.updatedAt = Instant.now(); }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
}
