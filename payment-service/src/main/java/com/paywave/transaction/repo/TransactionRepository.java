package com.paywave.transaction.repo;

import com.paywave.transaction.domain.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID>, JpaSpecificationExecutor<TransactionRecord> {
    Optional<TransactionRecord> findByPaymentIdAndOwnerIdAndDirectionAndReferenceType(UUID paymentId, UUID ownerId, String direction, String referenceType);
}
