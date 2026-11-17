package com.paywave.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "settlement_lines")
public class SettlementLine {
    @Id @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length = 36, nullable = false) private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "settlement_id", nullable = false, length = 36) private UUID settlementId;
    @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "payment_id", nullable = false, length = 36) private UUID paymentId;
    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2) private BigDecimal grossAmount;
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2) private BigDecimal feeAmount;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2) private BigDecimal refundAmount;
    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2) private BigDecimal netAmount;

    protected SettlementLine() {}

    public static SettlementLine of(UUID settlementId, UUID paymentId, BigDecimal gross, BigDecimal fee, BigDecimal refund, BigDecimal net) {
        SettlementLine line = new SettlementLine();
        line.id = UUID.randomUUID();
        line.settlementId = settlementId;
        line.paymentId = paymentId;
        line.grossAmount = gross;
        line.feeAmount = fee;
        line.refundAmount = refund;
        line.netAmount = net;
        return line;
    }

    public UUID getPaymentId() { return paymentId; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
}
