package com.paywave.payment.repo;

import com.paywave.payment.domain.Payment;
import com.paywave.payment.domain.PaymentStatus;
import com.paywave.payment.domain.PaymentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPayerIdAndIdempotencyKey(UUID payerId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select p from Payment p
            where p.payeeId = :merchantId
              and p.type = :type
              and p.settled = false
              and p.status in :statuses
              and p.createdAt < :before
            order by p.createdAt
            """)
    List<Payment> findUnsettled(@Param("merchantId") UUID merchantId,
                                @Param("type") PaymentType type,
                                @Param("statuses") Collection<PaymentStatus> statuses,
                                @Param("before") Instant before);
}
