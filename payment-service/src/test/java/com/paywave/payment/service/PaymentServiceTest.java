package com.paywave.payment.service;

import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.payment.client.LedgerClient;
import com.paywave.payment.client.MerchantClient;
import com.paywave.payment.client.TransactionClient;
import com.paywave.payment.client.WalletClient;
import com.paywave.payment.domain.PayeeType;
import com.paywave.payment.domain.Payment;
import com.paywave.payment.domain.PaymentStatus;
import com.paywave.payment.domain.PaymentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentStore store;
    @Mock WalletClient wallets;
    @Mock LedgerClient ledger;
    @Mock TransactionClient transactions;
    @Mock MerchantClient merchants;

    PaymentService service;
    PaymentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        service = new PaymentService(store, new GatewaySimulator(13, 14), wallets, ledger, transactions, merchants, properties);
    }

    @Test
    void declinedTopupDoesNotMoveMoney() {
        Payment payment = pendingTopup("10.13");
        when(store.createIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(payment);
        when(store.claim(payment.getId())).thenReturn(true);
        when(store.get(payment.getId())).thenReturn(payment);
        when(store.markFailed(any(), any(), any())).thenReturn(failed(payment));

        var view = service.topup(payment.getPayerId(), new BigDecimal("10.13"), "idem-1", null);

        assertThat(view.status()).isEqualTo("FAILED");
        verify(wallets, never()).credit(any());
        verify(ledger, never()).post(any(), any(), any(), any(), any());
    }

    @Test
    void transferCreditFailureCompensatesDebitAndLedger() {
        UUID payer = UUID.randomUUID();
        UUID payee = UUID.randomUUID();
        Payment payment = Payment.create(payer, "idem-2", payee, PayeeType.CUSTOMER, PaymentType.TRANSFER,
                new BigDecimal("40.00"), new BigDecimal("0.00"), new BigDecimal("40.00"), null, payee, "Customer");
        payment.setStatus(PaymentStatus.PENDING);
        when(store.createIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(payment);
        when(store.claim(payment.getId())).thenReturn(true);
        when(store.get(payment.getId())).thenReturn(payment);
        when(ledger.post(anyString(), anyString(), any(), anyString(), anyList())).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.doThrow(new PaywaveException(ErrorCode.UPSTREAM, "credit failed")).when(wallets).credit(any());
        when(store.markFailed(any(), any(), any())).thenReturn(failed(payment));

        var view = service.transfer(payer, payee, new BigDecimal("40.00"), "idem-2", null);

        assertThat(view.status()).isEqualTo("FAILED");
        verify(wallets).debit(any());
        verify(ledger).reverse(any(), anyString(), anyString());
        verify(store).markFailed(any(), any(), any());
    }

    @Test
    void duplicateSuccessDoesNotCallTheGatewayAgain() {
        Payment payment = pendingTopup("25.00");
        payment.setStatus(PaymentStatus.SUCCESS);
        when(store.createIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(payment);
        when(store.claim(payment.getId())).thenReturn(false);
        when(store.get(payment.getId())).thenReturn(payment);

        var view = service.topup(payment.getPayerId(), new BigDecimal("25.00"), "idem-3", null);
        assertThat(view.status()).isEqualTo("SUCCESS");
        verify(wallets, never()).credit(any());
    }

    private Payment pendingTopup(String amount) {
        Payment payment = Payment.create(UUID.randomUUID(), "idem", UUID.randomUUID(), PayeeType.PLATFORM, PaymentType.TOPUP,
                new BigDecimal(amount), new BigDecimal("0.00"), new BigDecimal(amount), null, UUID.randomUUID(), "Add money");
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Payment failed(Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        return payment;
    }
}
