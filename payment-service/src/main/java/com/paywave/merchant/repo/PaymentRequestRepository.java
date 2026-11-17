package com.paywave.merchant.repo;

import com.paywave.merchant.domain.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {}
