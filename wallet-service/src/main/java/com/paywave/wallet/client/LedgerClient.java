package com.paywave.wallet.client;

import com.paywave.ledger.domain.AccountType;
import com.paywave.ledger.service.LedgerService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class LedgerClient {

    private final LedgerService ledger;

    public LedgerClient(LedgerService ledger) {
        this.ledger = ledger;
    }

    public void openAccount(UUID ownerId, String ownerType, String currency) {
        AccountType accountType = "MERCHANT".equals(ownerType)
                ? AccountType.MERCHANT_PAYABLE
                : AccountType.CUSTOMER_WALLET;
        ledger.openAccount(ownerId, accountType, currency);
    }

    public Reconciliation reconcile(UUID ownerId, String accountType, BigDecimal reportedBalance) {
        LedgerService.Reconciliation result = ledger.reconcile(ownerId, AccountType.valueOf(accountType), reportedBalance);
        return new Reconciliation(result.ownerId(), result.accountType(), result.ledgerBalance(), result.reportedBalance(),
                result.difference(), result.matched());
    }

    public record Reconciliation(UUID ownerId, String accountType, BigDecimal ledgerBalance, BigDecimal reportedBalance,
                                 BigDecimal difference, boolean matched) {}
}
