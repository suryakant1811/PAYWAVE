package com.paywave.payment.repo;

import com.paywave.payment.domain.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
}
