package com.paywave.merchant.repo;

import com.paywave.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByOwnerUserId(UUID ownerUserId);
}
