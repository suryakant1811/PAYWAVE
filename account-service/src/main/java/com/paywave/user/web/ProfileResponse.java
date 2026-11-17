package com.paywave.user.web;

import com.paywave.user.domain.CustomerProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String addressLine,
        String city,
        String postalCode,
        String kycStatus,
        String accountStatus,
        String kycDocumentType,
        String kycDocumentLast4,
        Instant updatedAt
) {
    public static ProfileResponse from(CustomerProfile profile) {
        return new ProfileResponse(profile.getId(), profile.getFullName(), profile.getEmail(), profile.getPhone(),
                profile.getDateOfBirth(), profile.getAddressLine(), profile.getCity(), profile.getPostalCode(),
                profile.getKycStatus().name(), profile.getAccountStatus().name(), profile.getKycDocumentType(),
                profile.getKycDocumentLast4(), profile.getUpdatedAt());
    }
}
