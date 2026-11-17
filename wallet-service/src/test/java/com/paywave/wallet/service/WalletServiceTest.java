package com.paywave.wallet.service;

import com.paywave.common.error.PaywaveException;
import com.paywave.wallet.client.LedgerClient;
import com.paywave.wallet.config.WalletProperties;
import com.paywave.wallet.domain.Wallet;
import com.paywave.wallet.domain.WalletEntry;
import com.paywave.wallet.repo.WalletEntryRepository;
import com.paywave.wallet.repo.WalletRepository;
import com.paywave.wallet.web.WalletMutationRequest;
import com.paywave.web.outbox.OutboxWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock WalletRepository wallets;
    @Mock WalletEntryRepository entries;
    @Mock LedgerClient ledgerClient;
    @Mock OutboxWriter outboxWriter;

    WalletService service;
    Wallet wallet;

    @BeforeEach
    void setUp() {
        WalletProperties properties = new WalletProperties();
        service = new WalletService(wallets, entries, properties, ledgerClient, outboxWriter);
        wallet = Wallet.open(UUID.randomUUID(), com.paywave.wallet.domain.OwnerType.CUSTOMER, "INR",
                new BigDecimal("100000.00"), new BigDecimal("500000.00"), new BigDecimal("50000.00"));
        wallet.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void debitRejectsInsufficientBalanceAndDoesNotWriteAnEntry() {
        when(entries.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(wallets.findByOwnerIdForUpdate(wallet.getOwnerId())).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.debit(request("150.00")))
                .isInstanceOf(PaywaveException.class);
        verify(entries, never()).saveAndFlush(any());
        verify(outboxWriter, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void creditIsIdempotent() {
        WalletEntry entry = WalletEntry.of(wallet.getId(), "k2", com.paywave.wallet.domain.EntryType.CREDIT,
                new BigDecimal("25.00"), new BigDecimal("125.00"), "TOPUP", UUID.randomUUID());
        when(entries.findByIdempotencyKey("k2")).thenReturn(Optional.of(entry));

        var response = service.credit(request("25.00", "k2", entry.getReferenceId()));
        assertThat(response.entryId()).isEqualTo(entry.getId());
        assertThat(response.balanceAfter()).isEqualByComparingTo("125.00");
        verify(wallets, never()).findByOwnerIdForUpdate(any());
    }

    @Test
    void debitReducesBalance() {
        when(entries.findByIdempotencyKey("k3")).thenReturn(Optional.empty());
        when(wallets.findByOwnerIdForUpdate(wallet.getOwnerId())).thenReturn(Optional.of(wallet));
        when(entries.sumSince(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(entries.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.debit(request("40.00", "k3", UUID.randomUUID()));
        assertThat(response.balanceAfter()).isEqualByComparingTo("60.00");
        assertThat(wallet.getBalance()).isEqualByComparingTo("60.00");
    }

    private WalletMutationRequest request(String amount) {
        return request(amount, "k1", UUID.randomUUID());
    }

    private WalletMutationRequest request(String amount, String key, UUID referenceId) {
        return new WalletMutationRequest(wallet.getOwnerId(), "CUSTOMER", new BigDecimal(amount), key,
                "TRANSFER", referenceId, "INR", wallet.getOwnerId());
    }
}
