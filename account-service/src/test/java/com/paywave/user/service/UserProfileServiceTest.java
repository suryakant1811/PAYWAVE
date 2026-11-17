package com.paywave.user.service;

import com.paywave.user.domain.KycStatus;
import com.paywave.user.repo.CustomerProfileRepository;
import com.paywave.user.web.KycRequest;
import com.paywave.user.web.ProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock CustomerProfileRepository profiles;
    @InjectMocks UserProfileService service;

    @Test
    void kycVerifiesLongAlphanumericDocumentsAndStoresOnlyAHash() {
        UUID id = UUID.randomUUID();
        var profile = com.paywave.user.domain.CustomerProfile.create(id, "Ada", "a@paywave.test", "9876543210");
        when(profiles.findById(id)).thenReturn(Optional.of(profile));

        var verified = service.submitKyc(id, new KycRequest("PAN", "ABCDE1234F123"));
        assertThat(verified.kycStatus()).isEqualTo(KycStatus.VERIFIED.name());
        assertThat(verified.kycDocumentLast4()).isEqualTo("F123");
        assertThat(profile.getKycDocumentHash()).isNotEqualTo("ABCDE1234F123");

        var rejected = service.submitKyc(id, new KycRequest("PAN", "SHORT"));
        assertThat(rejected.kycStatus()).isEqualTo(KycStatus.REJECTED.name());
    }

    @Test
    void createIsIdempotent() {
        UUID id = UUID.randomUUID();
        var profile = com.paywave.user.domain.CustomerProfile.create(id, "Ada", "a@paywave.test", "9876543210");
        when(profiles.findById(id)).thenReturn(Optional.of(profile));
        var response = service.create(new ProfileRequest(id, "Ada", "a@paywave.test", "9876543210"));
        assertThat(response.userId()).isEqualTo(id);
        org.mockito.Mockito.verify(profiles, org.mockito.Mockito.never()).save(any());
    }
}
