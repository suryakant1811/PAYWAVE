package com.paywave.wallet.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.api.PageResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.ledger.PlatformAccounts;
import com.paywave.wallet.client.LedgerClient;
import com.paywave.wallet.service.WalletService;
import com.paywave.web.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class WalletController {

    private final WalletService wallets;
    private final LedgerClient ledgerClient;

    public WalletController(WalletService wallets, LedgerClient ledgerClient) {
        this.wallets = wallets;
        this.ledgerClient = ledgerClient;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Get the authenticated customer's wallet")
    @GetMapping("/api/v1/wallets/me")
    public ApiResponse<WalletView> me() {
        return ApiResponse.ok(wallets.getByOwner(SecurityUtils.currentUserId()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Wallet ledger entries for the authenticated customer")
    @GetMapping("/api/v1/wallets/me/entries")
    public ApiResponse<PageResponse<WalletEntryResponse>> entries(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        var result = wallets.entries(SecurityUtils.currentUserId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(new PageResponse<>(result.map(WalletEntryResponse::from).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Compare the wallet balance with the double-entry ledger")
    @GetMapping("/api/v1/wallets/me/reconciliation")
    public ApiResponse<LedgerClient.Reconciliation> reconciliation() {
        WalletView wallet = wallets.getByOwner(SecurityUtils.currentUserId());
        String accountType = "MERCHANT".equals(wallet.ownerType()) ? PlatformAccounts.MERCHANT_PAYABLE : PlatformAccounts.CUSTOMER_WALLET;
        return ApiResponse.ok(ledgerClient.reconcile(wallet.ownerId(), accountType, wallet.balance()));
    }

    @PostMapping("/internal/wallets")
    public ApiResponse<WalletView> create(@RequestBody CreateWalletBody body) {
        if (body.ownerId() == null || body.ownerType() == null) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "ownerId and ownerType are required");
        }
        return ApiResponse.ok(wallets.create(body.ownerId(), body.ownerType(), body.currency(), body.notifyUserId()));
    }

    @GetMapping("/internal/wallets/{ownerId}")
    public ApiResponse<WalletView> get(@PathVariable UUID ownerId) {
        return ApiResponse.ok(wallets.getByOwner(ownerId));
    }

    @PostMapping("/internal/wallets/credit")
    public ApiResponse<WalletMutationResponse> credit(@RequestBody WalletMutationRequest request) {
        validateMutation(request);
        return ApiResponse.ok(wallets.credit(request));
    }

    @PostMapping("/internal/wallets/debit")
    public ApiResponse<WalletMutationResponse> debit(@RequestBody WalletMutationRequest request) {
        validateMutation(request);
        return ApiResponse.ok(wallets.debit(request));
    }

    @DeleteMapping("/internal/wallets/{ownerId}")
    public ApiResponse<Map<String, String>> delete(@PathVariable UUID ownerId) {
        wallets.deleteIfEmpty(ownerId);
        return ApiResponse.ok(Map.of("status", "DELETED"));
    }

    private void validateMutation(WalletMutationRequest request) {
        if (request.ownerId() == null || request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                || request.referenceId() == null || request.referenceType() == null) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "ownerId, idempotencyKey, referenceType and referenceId are required");
        }
    }

    public record CreateWalletBody(UUID ownerId, String ownerType, String currency, UUID notifyUserId) {}
}
