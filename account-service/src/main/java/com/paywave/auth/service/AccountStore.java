package com.paywave.auth.service;

import com.paywave.auth.domain.AccountStatus;
import com.paywave.auth.domain.UserAccount;
import com.paywave.auth.repo.UserAccountRepository;
import com.paywave.common.error.ErrorCode;
import com.paywave.common.error.PaywaveException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountStore {

    private final UserAccountRepository users;

    public AccountStore(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return users.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserAccount require(UUID id) {
        return users.findById(id).orElseThrow(() -> new PaywaveException(ErrorCode.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserAccount createPending(String email, String phone, String passwordHash) {
        if (users.existsByEmail(email)) {
            throw new PaywaveException(ErrorCode.CONFLICT, "Email is already registered");
        }
        users.findByPhone(phone).ifPresent(existing -> {
            throw new PaywaveException(ErrorCode.CONFLICT, "Phone is already registered");
        });
        return users.save(UserAccount.create(UUID.randomUUID(), email, phone, passwordHash));
    }

    @Transactional
    public UserAccount activate(UUID id) {
        UserAccount user = require(id);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    @Transactional
    public UserAccount addRole(UUID id, String role) {
        UserAccount user = require(id);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new PaywaveException(ErrorCode.ACCOUNT_INACTIVE, "User is not active");
        }
        user.addRole(role);
        return user;
    }

    @Transactional
    public void deletePending(UUID id) {
        users.findById(id).ifPresent(user -> {
            if (user.getStatus() == AccountStatus.PENDING || user.getStatus() == AccountStatus.FAILED) {
                users.delete(user);
            }
        });
    }
}
