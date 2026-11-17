package com.paywave.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(length = 80)
    private String city;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "kyc_document_type", length = 32)
    private String kycDocumentType;

    @Column(name = "kyc_document_last4", length = 4)
    private String kycDocumentLast4;

    @Column(name = "kyc_document_hash", length = 64)
    private String kycDocumentHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerProfile() {
    }

    public static CustomerProfile create(UUID id, String fullName, String email, String phone) {
        CustomerProfile profile = new CustomerProfile();
        profile.id = id;
        profile.fullName = fullName;
        profile.email = email;
        profile.phone = phone;
        profile.kycStatus = KycStatus.PENDING;
        profile.accountStatus = AccountStatus.ACTIVE;
        Instant now = Instant.now();
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; touch(); }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; touch(); }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; touch(); }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; touch(); }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; touch(); }
    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; touch(); }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; touch(); }
    public String getKycDocumentType() { return kycDocumentType; }
    public void setKycDocumentType(String kycDocumentType) { this.kycDocumentType = kycDocumentType; }
    public String getKycDocumentLast4() { return kycDocumentLast4; }
    public void setKycDocumentLast4(String kycDocumentLast4) { this.kycDocumentLast4 = kycDocumentLast4; }
    public String getKycDocumentHash() { return kycDocumentHash; }
    public void setKycDocumentHash(String kycDocumentHash) { this.kycDocumentHash = kycDocumentHash; touch(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
