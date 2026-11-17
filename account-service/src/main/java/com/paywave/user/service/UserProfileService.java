package com.paywave.user.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.user.domain.AccountStatus;
import com.paywave.user.domain.CustomerProfile;
import com.paywave.user.domain.KycStatus;
import com.paywave.user.repo.CustomerProfileRepository;
import com.paywave.user.web.KycRequest;
import com.paywave.user.web.ProfileRequest;
import com.paywave.user.web.ProfileResponse;
import com.paywave.user.web.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class UserProfileService {

    private final CustomerProfileRepository profiles;

    public UserProfileService(CustomerProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional
    public ProfileResponse create(ProfileRequest request) {
        return profiles.findById(request.userId())
                .map(ProfileResponse::from)
                .orElseGet(() -> ProfileResponse.from(profiles.save(
                        CustomerProfile.create(request.userId(), request.fullName().trim(),
                                request.email().trim().toLowerCase(), request.phone().trim()))));
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID id) {
        return ProfileResponse.from(require(id));
    }

    @Transactional
    public ProfileResponse update(UUID id, UpdateProfileRequest request) {
        CustomerProfile profile = requireActive(id);
        profile.setFullName(request.fullName().trim());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setAddressLine(request.addressLine());
        profile.setCity(request.city());
        profile.setPostalCode(request.postalCode());
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse submitKyc(UUID id, KycRequest request) {
        CustomerProfile profile = requireActive(id);
        String document = request.documentNumber().trim().toUpperCase();
        profile.setKycDocumentType(request.documentType().trim().toUpperCase());
        profile.setKycDocumentLast4(document.substring(Math.max(0, document.length() - 4)));
        profile.setKycDocumentHash(sha256(document));
        boolean verified = document.length() >= 12 && document.matches("^[A-Z0-9]+$");
        profile.setKycStatus(verified ? KycStatus.VERIFIED : KycStatus.REJECTED);
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse updateStatus(UUID id, AccountStatus status) {
        CustomerProfile profile = require(id);
        profile.setAccountStatus(status);
        return ProfileResponse.from(profile);
    }

    @Transactional
    public void delete(UUID id) {
        profiles.findById(id).ifPresent(profiles::delete);
    }

    private CustomerProfile require(UUID id) {
        return profiles.findById(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Customer profile not found"));
    }

    private CustomerProfile requireActive(UUID id) {
        CustomerProfile profile = require(id);
        if (profile.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Customer account is not active");
        }
        return profile;
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
