package com.paywave.merchant.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.paywave.common.api.ApiResponse;
import com.paywave.common.api.PageResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.money.Money;
import com.paywave.merchant.domain.Merchant;
import com.paywave.merchant.domain.MerchantStatus;
import com.paywave.merchant.domain.PaymentRequest;
import com.paywave.merchant.repo.MerchantRepository;
import com.paywave.merchant.repo.PaymentRequestRepository;
import com.paywave.merchant.web.MerchantResponse;
import com.paywave.merchant.web.RegisterMerchantRequest;
import com.paywave.transaction.service.TransactionService;
import com.paywave.transaction.web.TransactionResponse;
import com.paywave.web.http.InternalRestClientFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchants;
    private final PaymentRequestRepository requests;
    private final TransactionService transactions;
    private final RestClient auth;
    private final RestClient wallet;

    public MerchantService(MerchantRepository merchants, PaymentRequestRepository requests, TransactionService transactions,
                           InternalRestClientFactory factory,
                           @Value("${paywave.clients.account-service-url}") String accountUrl,
                           @Value("${paywave.clients.wallet-service-url}") String walletUrl) {
        this.merchants = merchants;
        this.requests = requests;
        this.transactions = transactions;
        this.auth = factory.create(accountUrl);
        this.wallet = factory.create(walletUrl);
    }

    @Transactional
    public MerchantResponse register(UUID ownerUserId, RegisterMerchantRequest request) {
        Merchant existing = merchants.findByOwnerUserId(ownerUserId).orElse(null);
        if (existing != null && existing.getStatus() == MerchantStatus.ACTIVE) {
            return MerchantResponse.from(existing);
        }
        String account = request.settlementAccountNumber().trim();
        Merchant merchant = existing != null ? existing : merchants.save(Merchant.create(ownerUserId, request.businessName().trim(),
                request.category().trim(), request.contactEmail().trim().toLowerCase(),
                account.substring(account.length() - 4), sha256(account)));
        addMerchantRole(ownerUserId);
        try {
            openWallet(merchant.getId(), ownerUserId);
        } catch (RuntimeException ex) {
            if (existing == null) {
                merchants.delete(merchant);
            }
            throw ex;
        }
        merchant.setStatus(MerchantStatus.ACTIVE);
        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse requireByOwner(UUID ownerUserId) {
        return MerchantResponse.from(merchants.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Merchant profile not found")));
    }

    @Transactional
    public MerchantResponse update(UUID ownerUserId, String businessName, String category) {
        if (businessName == null || businessName.isBlank() || category == null || category.isBlank()) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "businessName and category are required");
        }
        Merchant merchant = owned(ownerUserId);
        merchant.setBusinessName(businessName.trim());
        merchant.setCategory(category.trim());
        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse get(UUID id) {
        return MerchantResponse.from(merchants.findById(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Merchant not found")));
    }

    public String qrPng(UUID ownerUserId) {
        Merchant merchant = owned(ownerUserId);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(merchant.getQrPayload(), BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            throw new PaywaveException(ErrorCode.INTERNAL, "Could not generate QR code");
        }
    }

    @Transactional
    public Map<String, Object> createPaymentRequest(UUID ownerUserId, BigDecimal amount, String note, int expiresInMinutes) {
        Merchant merchant = owned(ownerUserId);
        PaymentRequest request = requests.save(PaymentRequest.create(merchant.getId(), Money.normalize(amount), note,
                Instant.now().plus(Math.max(expiresInMinutes, 5), ChronoUnit.MINUTES)));
        String payload = merchant.getQrPayload() + "&requestId=" + request.getId()
                + "&amount=" + URLEncoder.encode(request.getAmount().toPlainString(), StandardCharsets.UTF_8);
        return Map.of("requestId", request.getId(), "merchantId", merchant.getId(), "amount", request.getAmount(),
                "expiresAt", request.getExpiresAt(), "payload", payload);
    }

    public Object history(UUID ownerUserId, int page, int size) {
        Merchant merchant = owned(ownerUserId);
        var result = transactions.search(merchant.getId(), null, null, null, null, null, null,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new PageResponse<>(result.map(TransactionResponse::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private Merchant owned(UUID ownerUserId) {
        Merchant merchant = merchants.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "Merchant profile not found"));
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Merchant is not active");
        }
        return merchant;
    }

    @Retry(name = "authService")
    @CircuitBreaker(name = "authService")
    protected void addMerchantRole(UUID userId) {
        call(auth, () -> auth.post().uri("/internal/users/{id}/roles", userId).body(Map.of("role", "MERCHANT")).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    @Retry(name = "walletService")
    @CircuitBreaker(name = "walletService")
    protected void openWallet(UUID merchantId, UUID ownerUserId) {
        call(wallet, () -> wallet.post().uri("/internal/wallets")
                .body(Map.of("ownerId", merchantId, "ownerType", "MERCHANT", "currency", "INR", "notifyUserId", ownerUserId))
                .retrieve().body(new ParameterizedTypeReference<ApiResponse<Object>>() {}));
    }

    private void call(RestClient ignored, java.util.function.Supplier<ApiResponse<Object>> action) {
        try {
            ApiResponse<Object> response = action.get();
            if (response == null || !response.success()) {
                throw new PaywaveException(ErrorCode.UPSTREAM, "Downstream call failed");
            }
        } catch (PaywaveException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PaywaveException(ErrorCode.UPSTREAM, "Downstream service is unreachable");
        }
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
