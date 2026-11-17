package com.paywave.ledger.repo;

import com.paywave.ledger.domain.AccountType;
import com.paywave.ledger.domain.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    Optional<LedgerAccount> findByOwnerIdAndAccountTypeAndCurrency(UUID ownerId, AccountType accountType, String currency);
}
