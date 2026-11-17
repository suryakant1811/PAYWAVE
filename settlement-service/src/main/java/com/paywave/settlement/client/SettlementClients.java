package com.paywave.settlement.client;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.web.http.InternalRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SettlementClients {
    private final RestClient payments;
    private final RestClient wallets;
    private final RestClient ledger;
    private final RestClient transactions;
    private final RestClient merchants;

    public SettlementClients(InternalRestClientFactory factory,
                             @Value("${paywave.clients.payment-service-url}") String paymentUrl,
                             @Value("${paywave.clients.wallet-service-url}") String walletUrl) {
        this.payments = factory.create(paymentUrl);
        this.wallets = factory.create(walletUrl);
        this.ledger = factory.create(walletUrl);
        this.transactions = factory.create(paymentUrl);
        this.merchants = factory.create(paymentUrl);
    }

    public MerchantSnapshot merchantForOwner(UUID ownerUserId) {
        return data(merchants.get().uri("/internal/merchants/by-owner/{id}", ownerUserId).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<MerchantSnapshot>>() {}));
    }

    public MerchantSnapshot merchant(UUID merchantId) {
        return data(merchants.get().uri("/internal/merchants/{id}", merchantId).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<MerchantSnapshot>>() {}));
    }

    public List<PaymentSnapshot> unsettled(UUID merchantId, LocalDate through) {
        ApiResponse<List<PaymentSnapshot>> response = payments.get()
                .uri(uri -> uri.path("/internal/payments/unsettled").queryParam("merchantId", merchantId).queryParam("through", through).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || !response.success() || response.data() == null) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "payment-service returned an empty settlement preview");
        }
        return response.data();
    }

    public void markSettled(UUID settlementId, List<UUID> paymentIds) {
        data(payments.post().uri("/internal/payments/settlements").body(Map.of("settlementId", settlementId, "paymentIds", paymentIds))
                .retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    public void debitWallet(UUID merchantId, BigDecimal amount, String key, UUID settlementId, UUID notifyUserId) {
        data(wallets.post().uri("/internal/wallets/debit").body(Map.of(
                "ownerId", merchantId, "ownerType", "MERCHANT", "amount", amount, "idempotencyKey", key,
                "referenceType", "SETTLEMENT", "referenceId", settlementId, "currency", "INR", "notifyUserId", notifyUserId
        )).retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    public void journal(String key, UUID settlementId, UUID merchantId, BigDecimal net) {
        data(ledger.post().uri("/internal/ledger/journals").body(Map.of(
                "idempotencyKey", key, "referenceType", "SETTLEMENT", "referenceId", settlementId,
                "description", "Merchant settlement",
                "lines", List.of(
                        Map.of("ownerId", merchantId, "accountType", "MERCHANT_PAYABLE", "direction", "DEBIT", "amount", net, "currency", "INR"),
                        Map.of("ownerId", com.paywave.common.ledger.PlatformAccounts.PAYOUT_CLEARING, "accountType", "PAYOUT_CLEARING", "direction", "CREDIT", "amount", net, "currency", "INR")
                )
        )).retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    public void history(UUID settlementId, UUID merchantId, BigDecimal net) {
        data(transactions.post().uri("/internal/transactions/batch").body(List.of(Map.of(
                "paymentId", settlementId, "ownerId", merchantId, "counterpartyId", settlementId,
                "counterpartyName", "PayWave settlement", "direction", "DEBIT", "type", "SETTLEMENT",
                "amount", net, "status", "SUCCESS", "description", "Daily settlement payout", "referenceType", "SETTLEMENT"
        ))).retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    private <T> T data(ApiResponse<T> response) {
        if (response == null || !response.success()) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "Downstream settlement call failed");
        }
        return response.data();
    }

    public record MerchantSnapshot(UUID merchantId, UUID ownerUserId, String businessName, String status) {}
    public record PaymentSnapshot(UUID paymentId, UUID payerId, UUID payeeId, String payeeType, String type,
                                  BigDecimal amount, BigDecimal feeAmount, BigDecimal netAmount, BigDecimal refundedAmount,
                                  String currency, String status, String failureReason, String gatewayReference,
                                  boolean settled, Instant createdAt) {}
}
