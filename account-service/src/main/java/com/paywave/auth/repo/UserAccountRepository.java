package com.paywave.auth.repo;

import com.paywave.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<UserAccount> findByPhone(String phone);
}
