package com.paywave.ledger.service;

import com.paywave.common.error.PaywaveException;
import com.paywave.ledger.domain.AccountType;
import com.paywave.ledger.domain.JournalEntry;
import com.paywave.ledger.domain.LedgerAccount;
import com.paywave.ledger.repo.JournalEntryRepository;
import com.paywave.ledger.repo.JournalLineRepository;
import com.paywave.ledger.repo.LedgerAccountRepository;
import com.paywave.ledger.web.PostJournalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock LedgerAccountRepository accounts;
    @Mock JournalEntryRepository journals;
    @Mock JournalLineRepository lines;

    LedgerService service;

    @BeforeEach
    void setUp() {
        service = new LedgerService(accounts, journals, lines);
    }

    @Test
    void unbalancedJournalIsRejected() {
        UUID owner = UUID.randomUUID();
        LedgerAccount account = LedgerAccount.open(owner, AccountType.CUSTOMER_WALLET, "INR");
        when(journals.findByIdempotencyKey("j1")).thenReturn(Optional.empty());
        when(accounts.findByOwnerIdAndAccountTypeAndCurrency(owner, AccountType.CUSTOMER_WALLET, "INR")).thenReturn(Optional.of(account));
        PostJournalRequest request = new PostJournalRequest("j1", "PAYMENT", UUID.randomUUID(), "bad", List.of(
                new PostJournalRequest.Line(owner, "CUSTOMER_WALLET", "DEBIT", new BigDecimal("10.00"), "INR"),
                new PostJournalRequest.Line(owner, "CUSTOMER_WALLET", "CREDIT", new BigDecimal("9.00"), "INR")
        ));
        assertThatThrownBy(() -> service.post(request)).isInstanceOf(PaywaveException.class);
        verify(journals, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingJournal() {
        JournalEntry entry = JournalEntry.post("j2", "PAYMENT", UUID.randomUUID(), null, "existing");
        when(journals.findByIdempotencyKey("j2")).thenReturn(Optional.of(entry));
        when(lines.findByJournalEntryId(entry.getId())).thenReturn(List.of());
        var response = service.post(new PostJournalRequest("j2", "PAYMENT", entry.getReferenceId(), "existing", List.of()));
        org.assertj.core.api.Assertions.assertThat(response.journalId()).isEqualTo(entry.getId());
        verify(journals, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}
