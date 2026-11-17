package com.paywave.auth.service;

import com.paywave.auth.client.UserServiceClient;
import com.paywave.auth.client.WalletServiceClient;
import com.paywave.auth.domain.AccountStatus;
import com.paywave.auth.domain.UserAccount;
import com.paywave.auth.repo.LoginAuditRepository;
import com.paywave.auth.web.RegisterRequest;
import com.paywave.common.error.PaywaveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AccountStore accounts;
    @Mock TokenService tokens;
    @Mock UserServiceClient userServiceClient;
    @Mock WalletServiceClient walletServiceClient;
    @Mock LoginAuditRepository loginAudits;

    AuthService authService;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        authService = new AuthService(accounts, tokens, encoder, userServiceClient, walletServiceClient, loginAudits);
    }

    @Test
    void registerCreatesProfileWalletAndTokens() {
        UserAccount pending = UserAccount.create(UUID.randomUUID(), "a@paywave.test", "9876543210", encoder.encode("Secret123"));
        UserAccount active = UserAccount.create(pending.getId(), pending.getEmail(), pending.getPhone(), pending.getPasswordHash());
        active.setStatus(AccountStatus.ACTIVE);
        when(accounts.findByEmail("a@paywave.test")).thenReturn(Optional.empty());
        when(accounts.createPending(any(), any(), any())).thenReturn(pending);
        when(accounts.activate(pending.getId())).thenReturn(active);
        when(tokens.issue(active)).thenReturn(new TokenService.IssuedTokens("access", "refresh", 900));

        var response = authService.register(new RegisterRequest("Ada Lovelace", "A@paywave.test", "9876543210", "Secret123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.userId()).isEqualTo(pending.getId());
        verify(userServiceClient).createProfile(any());
        verify(walletServiceClient).createWallet(any());
    }

    @Test
    void registerCompensatesWhenWalletCreationFails() {
        UserAccount pending = UserAccount.create(UUID.randomUUID(), "a@paywave.test", "9876543210", encoder.encode("Secret123"));
        when(accounts.findByEmail("a@paywave.test")).thenReturn(Optional.empty());
        when(accounts.createPending(any(), any(), any())).thenReturn(pending);
        org.mockito.Mockito.doThrow(new PaywaveException(com.paywave.common.error.ErrorCode.UPSTREAM, "down"))
                .when(walletServiceClient).createWallet(any());

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Ada", "a@paywave.test", "9876543210", "Secret123")))
                .isInstanceOf(PaywaveException.class);
        verify(userServiceClient).deleteProfile(pending.getId());
        verify(accounts).deletePending(pending.getId());
        verify(tokens, never()).issue(any());
    }
}
