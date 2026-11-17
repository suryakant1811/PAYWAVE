package com.paywave.ledger.web;

import com.paywave.common.api.ApiResponse;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.ledger.PlatformAccounts;
import com.paywave.ledger.domain.AccountType;
import com.paywave.ledger.service.LedgerService;
import com.paywave.web.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class LedgerController {

    private final LedgerService ledger;

    public LedgerController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Customer wallet balance according to the ledger")
    @GetMapping("/api/v1/ledger/me")
    public ApiResponse<Map<String, Object>> me() {
        BigDecimal balance = ledger.balance(SecurityUtils.currentUserId(), AccountType.CUSTOMER_WALLET);
        return ApiResponse.ok(Map.of("ownerId", SecurityUtils.currentUserId(), "accountType", "CUSTOMER_WALLET", "balance", balance, "currency", "INR"));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Journals posted for a business reference")
    @GetMapping("/api/v1/ledger/journals")
    public ApiResponse<List<JournalResponse>> journals(@RequestParam String referenceType, @RequestParam UUID referenceId) {
        return ApiResponse.ok(ledger.byReference(referenceType, referenceId));
    }

    @PostMapping("/internal/ledger/accounts")
    public ApiResponse<Map<String, Object>> open(@RequestBody OpenAccountRequest request) {
        var account = ledger.openAccount(request.ownerId(), AccountType.valueOf(request.accountType()), request.currency());
        return ApiResponse.ok(Map.of("accountId", account.getId(), "accountType", account.getAccountType().name(), "ownerId", account.getOwnerId()));
    }

    @PostMapping("/internal/ledger/journals")
    public ApiResponse<JournalResponse> post(@RequestBody PostJournalRequest request) {
        if (request.idempotencyKey() == null || request.referenceId() == null || request.referenceType() == null) {
            throw new PaywaveException(ErrorCode.VALIDATION_ERROR, "idempotencyKey, referenceType and referenceId are required");
        }
        return ApiResponse.ok(ledger.post(request));
    }

    @PostMapping("/internal/ledger/journals/reverse")
    public ApiResponse<JournalResponse> reverse(@RequestBody ReverseRequest request) {
        return ApiResponse.ok(ledger.reverse(request.journalId(), request.idempotencyKey(), request.description()));
    }

    @GetMapping("/internal/ledger/balance")
    public ApiResponse<Map<String, Object>> balance(@RequestParam UUID ownerId, @RequestParam String accountType) {
        BigDecimal balance = ledger.balance(ownerId, AccountType.valueOf(accountType));
        return ApiResponse.ok(Map.of("ownerId", ownerId, "accountType", accountType, "balance", balance));
    }

    @PostMapping("/internal/ledger/reconcile")
    public ApiResponse<LedgerService.Reconciliation> reconcile(@RequestBody ReconcileRequest request) {
        return ApiResponse.ok(ledger.reconcile(request.ownerId(), AccountType.valueOf(request.accountType()), request.reportedBalance()));
    }

    @GetMapping("/internal/ledger/platform-accounts")
    public ApiResponse<Map<String, String>> platform() {
        return ApiResponse.ok(Map.of(
                "gatewayClearing", PlatformAccounts.GATEWAY_CLEARING.toString(),
                "feeIncome", PlatformAccounts.FEE_INCOME.toString(),
                "payoutClearing", PlatformAccounts.PAYOUT_CLEARING.toString()));
    }

    public record OpenAccountRequest(UUID ownerId, String accountType, String currency) {}
    public record ReverseRequest(UUID journalId, String idempotencyKey, String description) {}
    public record ReconcileRequest(UUID ownerId, String accountType, String currency, BigDecimal reportedBalance) {}
}
