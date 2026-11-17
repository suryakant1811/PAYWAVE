package com.paywave.settlement.repo;

import com.paywave.settlement.domain.SettlementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, UUID> {
    List<SettlementLine> findBySettlementId(UUID settlementId);
}
