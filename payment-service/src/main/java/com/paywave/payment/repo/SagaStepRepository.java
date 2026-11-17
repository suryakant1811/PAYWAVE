package com.paywave.payment.repo;

import com.paywave.payment.domain.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {
}
