package com.paywave.wallet.repo;

import com.paywave.wallet.domain.EntryType;
import com.paywave.wallet.domain.WalletEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WalletEntryRepository extends JpaRepository<WalletEntry, UUID> {
    Optional<WalletEntry> findByIdempotencyKey(String idempotencyKey);
    Page<WalletEntry> findByWalletId(UUID walletId, Pageable pageable);
    long countByWalletId(UUID walletId);

    @Query("""
            select coalesce(sum(e.amount), 0) from WalletEntry e
            where e.walletId = :walletId and e.type = :type and e.createdAt >= :since
            """)
    BigDecimal sumSince(@Param("walletId") UUID walletId, @Param("type") EntryType type, @Param("since") Instant since);

    @Query("""
            select coalesce(sum(e.amount), 0) from WalletEntry e
            where e.walletId = :walletId and e.type = :type and e.referenceType = :referenceType and e.createdAt >= :since
            """)
    BigDecimal sumSinceForReference(@Param("walletId") UUID walletId,
                                    @Param("type") EntryType type,
                                    @Param("referenceType") String referenceType,
                                    @Param("since") Instant since);
}
