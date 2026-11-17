package com.paywave.merchant.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.merchant.service.MerchantService;
import com.paywave.web.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
public class MerchantController {
    private final MerchantService merchants;
    public MerchantController(MerchantService merchants) { this.merchants = merchants; }

    @io.swagger.v3.oas.annotations.Operation(summary = "Register the authenticated user as a merchant and open a merchant wallet")
    @PostMapping("/api/v1/merchants/register")
    public ApiResponse<MerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request) {
        return ApiResponse.ok(merchants.register(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/api/v1/merchants/me")
    public ApiResponse<MerchantResponse> me() { return ApiResponse.ok(merchants.requireByOwner(SecurityUtils.currentUserId())); }

    @PutMapping("/api/v1/merchants/me")
    public ApiResponse<MerchantResponse> update(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(merchants.update(SecurityUtils.currentUserId(), body.get("businessName"), body.get("category")));
    }

    @GetMapping("/api/v1/merchants/me/qr")
    public ApiResponse<Map<String, String>> qr() {
        MerchantResponse merchant = merchants.requireByOwner(SecurityUtils.currentUserId());
        return ApiResponse.ok(Map.of("merchantId", merchant.merchantId().toString(), "payload", merchant.qrPayload(),
                "pngBase64", merchants.qrPng(SecurityUtils.currentUserId())));
    }

    @PostMapping("/api/v1/merchants/me/payment-requests")
    public ApiResponse<Map<String, Object>> request(@RequestBody PaymentRequestBody body) {
        return ApiResponse.ok(merchants.createPaymentRequest(SecurityUtils.currentUserId(), body.amount(), body.note(),
                body.expiresInMinutes() == null ? 30 : body.expiresInMinutes()));
    }

    @GetMapping("/api/v1/merchants/me/transactions")
    public ApiResponse<Object> transactions(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(merchants.history(SecurityUtils.currentUserId(), page, size));
    }

    @GetMapping("/api/v1/merchants/{id}")
    public ApiResponse<MerchantResponse> get(@PathVariable UUID id) { return ApiResponse.ok(merchants.get(id)); }

    @GetMapping("/internal/merchants/{id}")
    public ApiResponse<MerchantResponse> internalGet(@PathVariable UUID id) { return ApiResponse.ok(merchants.get(id)); }

    @GetMapping("/internal/merchants/by-owner/{ownerUserId}")
    public ApiResponse<MerchantResponse> byOwner(@PathVariable UUID ownerUserId) { return ApiResponse.ok(merchants.requireByOwner(ownerUserId)); }

    public record PaymentRequestBody(@NotNull BigDecimal amount, String note, Integer expiresInMinutes) {}
}
