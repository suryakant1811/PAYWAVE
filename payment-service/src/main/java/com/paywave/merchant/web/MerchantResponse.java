package com.paywave.merchant.web;

import com.paywave.merchant.domain.Merchant;
import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID merchantId, UUID ownerUserId, String businessName, String category, String contactEmail,
        String settlementAccountLast4, String status, String qrPayload, Instant createdAt
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(merchant.getId(), merchant.getOwnerUserId(), merchant.getBusinessName(), merchant.getCategory(),
                merchant.getContactEmail(), merchant.getSettlementAccountLast4(), merchant.getStatus().name(),
                merchant.getQrPayload(), merchant.getCreatedAt());
    }
}
