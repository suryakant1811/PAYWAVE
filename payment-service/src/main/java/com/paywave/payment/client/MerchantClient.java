package com.paywave.payment.client;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.merchant.service.MerchantService;
import com.paywave.merchant.web.MerchantResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MerchantClient {

    private final MerchantService merchants;

    public MerchantClient(MerchantService merchants) {
        this.merchants = merchants;
    }

    public MerchantView get(UUID merchantId) {
        MerchantResponse merchant = merchants.get(merchantId);
        if (!"ACTIVE".equals(merchant.status())) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Merchant is not active");
        }
        return new MerchantView(merchant.merchantId(), merchant.ownerUserId(), merchant.businessName(), merchant.status());
    }

    public record MerchantView(UUID merchantId, UUID ownerUserId, String businessName, String status) {}
}
