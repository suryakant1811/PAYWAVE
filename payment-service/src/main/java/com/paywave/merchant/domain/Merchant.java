package com.paywave.merchant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "owner_user_id", nullable = false, unique = true, length = 36)
    private UUID ownerUserId;
    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;
    @Column(name = "settlement_account_last4", nullable = false, length = 4)
    private String settlementAccountLast4;
    @Column(name = "settlement_account_hash", nullable = false, length = 64)
    private String settlementAccountHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantStatus status;
    @Column(name = "qr_payload", nullable = false, length = 500)
    private String qrPayload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Merchant() {}

    public static Merchant create(UUID ownerUserId, String businessName, String category, String contactEmail, String last4, String hash) {
        Merchant merchant = new Merchant();
        merchant.id = UUID.randomUUID();
        merchant.ownerUserId = ownerUserId;
        merchant.businessName = businessName;
        merchant.category = category;
        merchant.contactEmail = contactEmail;
        merchant.settlementAccountLast4 = last4;
        merchant.settlementAccountHash = hash;
        merchant.status = MerchantStatus.PENDING;
        merchant.qrPayload = "paywave://pay?merchantId=" + merchant.id;
        Instant now = Instant.now();
        merchant.createdAt = now;
        merchant.updatedAt = now;
        return merchant;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; touch(); }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; touch(); }
    public String getContactEmail() { return contactEmail; }
    public String getSettlementAccountLast4() { return settlementAccountLast4; }
    public String getSettlementAccountHash() { return settlementAccountHash; }
    public MerchantStatus getStatus() { return status; }
    public void setStatus(MerchantStatus status) { this.status = status; touch(); }
    public String getQrPayload() { return qrPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    private void touch() { this.updatedAt = Instant.now(); }
}
