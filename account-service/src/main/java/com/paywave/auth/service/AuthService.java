package com.paywave.auth.service;

import com.paywave.auth.client.CreateProfileRequest;
import com.paywave.auth.client.CreateWalletRequest;
import com.paywave.auth.client.UserServiceClient;
import com.paywave.auth.client.WalletServiceClient;
import com.paywave.auth.domain.AccountStatus;
import com.paywave.auth.domain.LoginAudit;
import com.paywave.auth.domain.UserAccount;
import com.paywave.auth.repo.LoginAuditRepository;
import com.paywave.auth.web.AuthResponse;
import com.paywave.auth.web.LoginRequest;
import com.paywave.auth.web.RegisterRequest;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import com.paywave.common.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AccountStore accounts;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final LoginAuditRepository loginAudits;

    public AuthService(AccountStore accounts,
                       TokenService tokens,
                       PasswordEncoder passwordEncoder,
                       UserServiceClient userServiceClient,
                       WalletServiceClient walletServiceClient,
                       LoginAuditRepository loginAudits) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.walletServiceClient = walletServiceClient;
        this.loginAudits = loginAudits;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        String fullName = request.fullName().trim();
        UserAccount existing = accounts.findByEmail(email).orElse(null);
        UserAccount user;
        if (existing == null) {
            user = accounts.createPending(email, phone, passwordEncoder.encode(request.password()));
        } else if (existing.getStatus() == AccountStatus.ACTIVE) {
            throw new PaywaveException(ErrorCode.CONFLICT, "Email is already registered");
        } else if (!passwordEncoder.matches(request.password(), existing.getPasswordHash())) {
            throw new PaywaveException(ErrorCode.CONFLICT, "Email is already registered");
        } else {
            user = existing;
        }
        boolean profileCreated = false;
        boolean walletCreated = false;
        try {
            userServiceClient.createProfile(new CreateProfileRequest(user.getId(), fullName, email, phone));
            profileCreated = true;
            walletServiceClient.createWallet(new CreateWalletRequest(user.getId(), "CUSTOMER", Money.CURRENCY, user.getId()));
            walletCreated = true;
            UserAccount active = accounts.activate(user.getId());
            TokenService.IssuedTokens issued = tokens.issue(active);
            log.info("Registered customer {}", active.getId());
            return AuthResponse.from(active, issued);
        } catch (RuntimeException ex) {
            log.warn("Registration saga failed for {}: {}", user.getId(), ex.getMessage());
            if (walletCreated) {
                compensate(() -> walletServiceClient.deleteWallet(user.getId()));
            }
            if (profileCreated) {
                compensate(() -> userServiceClient.deleteProfile(user.getId()));
            }
            accounts.deletePending(user.getId());
            if (ex instanceof PaywaveException paywaveException) {
                throw paywaveException;
            }
            throw new PaywaveException(ErrorCode.UPSTREAM, "Registration could not be completed");
        }
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = accounts.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAudits.save(LoginAudit.of(user == null ? null : user.getId(), email, false, ipAddress));
            throw new PaywaveException(ErrorCode.UNAUTHORIZED, "Email or password is incorrect");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            loginAudits.save(LoginAudit.of(user.getId(), email, false, ipAddress));
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Account is not active");
        }
        loginAudits.save(LoginAudit.of(user.getId(), email, true, ipAddress));
        return AuthResponse.from(user, tokens.issue(user));
    }

    public AuthResponse refresh(String refreshToken) {
        var stored = tokens.requireActive(refreshToken);
        UserAccount user = accounts.require(stored.getUserId());
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "Account is not active");
        }
        return AuthResponse.from(user, tokens.rotate(refreshToken, user));
    }

    public void logout(String refreshToken) {
        tokens.revoke(refreshToken);
    }

    public UserAccount addRole(java.util.UUID userId, String role) {
        return accounts.addRole(userId, role);
    }

    public UserAccount current(java.util.UUID userId) {
        return accounts.require(userId);
    }

    private void compensate(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.error("Registration compensation failed: {}", ex.getMessage());
        }
    }
}
