package com.paywave.refund.client;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.ledger.PlatformAccounts;
import com.paywave.merchant.service.MerchantService;
import com.paywave.merchant.web.MerchantResponse;
import com.paywave.payment.service.PaymentService;
import com.paywave.payment.web.PaymentView;
import com.paywave.transaction.service.TransactionService;
import com.paywave.transaction.web.TransactionRequest;
import com.paywave.web.http.InternalRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RefundClients {
    private final PaymentService payments;
    private final MerchantService merchants;
    private final TransactionService transactions;
    private final RestClient wallets;
    private final RestClient ledger;

    public RefundClients(PaymentService payments,
                         MerchantService merchants,
                         TransactionService transactions,
                         InternalRestClientFactory factory,
                         @Value("${paywave.clients.wallet-service-url}") String walletUrl) {
        this.payments = payments;
        this.merchants = merchants;
        this.transactions = transactions;
        this.wallets = factory.create(walletUrl);
        this.ledger = factory.create(walletUrl);
    }

    public PaymentSnapshot payment(UUID id) {
        PaymentView view = PaymentView.from(payments.getInternal(id));
        return new PaymentSnapshot(view.paymentId(), view.payerId(), view.payeeId(), view.payeeType(), view.type(),
                view.amount(), view.feeAmount(), view.netAmount(), view.refundedAmount(), view.currency(),
                view.status(), view.failureReason(), view.gatewayReference(), view.settled(), view.createdAt());
    }

    public void applyRefund(UUID paymentId, UUID refundId, BigDecimal amount) {
        payments.applyRefund(paymentId, refundId, amount);
    }

    public MerchantSnapshot merchant(UUID id) {
        MerchantResponse merchant = merchants.get(id);
        return new MerchantSnapshot(merchant.merchantId(), merchant.ownerUserId(), merchant.businessName(), merchant.status());
    }

    public void wallet(String path, UUID ownerId, String ownerType, BigDecimal amount, String key, String referenceType, UUID referenceId, UUID notifyUserId) {
        data(wallets.post().uri(path).body(Map.of(
                "ownerId", ownerId, "ownerType", ownerType, "amount", amount, "idempotencyKey", key,
                "referenceType", referenceType, "referenceId", referenceId, "currency", "INR", "notifyUserId", notifyUserId
        )).retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}), "wallet-service");
    }

    public void journal(String key, UUID referenceId, String description, List<Map<String, Object>> lines) {
        data(ledger.post().uri("/internal/ledger/journals").body(Map.of(
                "idempotencyKey", key, "referenceType", "REFUND", "referenceId", referenceId, "description", description, "lines", lines
        )).retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}), "wallet-service");
    }

    public void history(List<Map<String, Object>> entries) {
        List<TransactionRequest> requests = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            requests.add(new TransactionRequest(
                    (UUID) entry.get("paymentId"),
                    (UUID) entry.get("ownerId"),
                    (UUID) entry.get("counterpartyId"),
                    (String) entry.get("counterpartyName"),
                    (String) entry.get("direction"),
                    (String) entry.get("type"),
                    (BigDecimal) entry.get("amount"),
                    (String) entry.get("status"),
                    (String) entry.get("description"),
                    (String) entry.get("referenceType")));
        }
        transactions.recordAll(requests);
    }

    public static Map<String, Object> line(UUID ownerId, String accountType, String direction, BigDecimal amount) {
        return Map.of("ownerId", ownerId, "accountType", accountType, "direction", direction, "amount", amount, "currency", "INR");
    }

    public static String gateway() { return PlatformAccounts.GATEWAY_CLEARING_TYPE; }

    private <T> T data(ApiResponse<T> response, String service) {
        try {
            if (response == null || !response.success() || response.data() == null) {
                throw new PaywaveException(ErrorCode.UPSTREAM, service + " returned an empty response");
            }
            return response.data();
        } catch (PaywaveException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaywaveException(ErrorCode.UPSTREAM, service + " is unreachable");
        }
    }

    public record PaymentSnapshot(UUID paymentId, UUID payerId, UUID payeeId, String payeeType, String type, BigDecimal amount,
                                  BigDecimal feeAmount, BigDecimal netAmount, BigDecimal refundedAmount, String currency,
                                  String status, String failureReason, String gatewayReference, boolean settled, Instant createdAt) {}

    public record MerchantSnapshot(UUID merchantId, UUID ownerUserId, String businessName, String status) {}
}
