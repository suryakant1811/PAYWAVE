package com.paywave.settlement.repo;

import com.paywave.settlement.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    Optional<Settlement> findByMerchantIdAndPeriodDate(UUID merchantId, LocalDate periodDate);
    List<Settlement> findByMerchantIdOrderByPeriodDateDesc(UUID merchantId);
}
